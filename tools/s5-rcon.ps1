# S5 RCON minimal client - Carpet TIS Addition NeoForge acceptance channel
# Packet: [int32 LE length][int32 LE request id][int32 LE type][body UTF8][0x00][0x00]
#         length = 4 + 4 + bodyByteCount + 2
#         type 3 = login, type 2 = command; response type 0 carries the result body
# Usage:  tools\s5-rcon.ps1 -Command "/tis"
#         tools\s5-rcon.ps1 -Command "/list" -Server 127.0.0.1 -Port 25575
# NOTE: keep this file ASCII-only; the host PowerShell parses non-ASCII source as ANSI.

param(
    [Parameter(Mandatory = $true)][string]$Command,
    [string]$Server = '127.0.0.1',
    [int]$Port = 25575,
    [string]$Password = 'tis-accept',
    [int]$TimeoutMs = 10000
)

$ErrorActionPreference = 'Stop'

function New-RconPacket {
    param([int]$PacketId, [int]$Type, [string]$Body)
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $len = 4 + 4 + $bodyBytes.Length + 2
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter($ms)
    $bw.Write([int]$len)
    $bw.Write([int]$PacketId)
    $bw.Write([int]$Type)
    $bw.Write($bodyBytes)
    $bw.Write([byte]0)
    $bw.Write([byte]0)
    $bw.Flush()
    return $ms.ToArray()
}

function Read-Exact {
    param([System.IO.Stream]$Stream, [int]$Count)
    $buf = New-Object byte[] $Count
    $got = 0
    while ($got -lt $Count) {
        $n = $Stream.Read($buf, $got, $Count - $got)
        if ($n -le 0) { throw ("RCON: connection closed while reading " + $Count + " bytes (got " + $got + ")") }
        $got += $n
    }
    return $buf
}

function Read-RconPacket {
    param([System.IO.Stream]$Stream)
    $lenBytes = Read-Exact -Stream $Stream -Count 4
    $size = [System.BitConverter]::ToInt32($lenBytes, 0)
    if ($size -lt 10 -or $size -gt 65536) { throw ("RCON: bad length field size=" + $size) }
    $payload = Read-Exact -Stream $Stream -Count $size
    $id = [System.BitConverter]::ToInt32($payload, 0)
    $type = [System.BitConverter]::ToInt32($payload, 4)
    $bodyLen = $size - 10
    $body = ''
    if ($bodyLen -gt 0) {
        $body = [System.Text.Encoding]::UTF8.GetString($payload, 8, $bodyLen)
    }
    return [pscustomobject]@{ Id = $id; Type = $type; Body = $body }
}

$client = New-Object System.Net.Sockets.TcpClient
try {
    $iar = $client.BeginConnect($Server, $Port, $null, $null)
    if (-not $iar.AsyncWaitHandle.WaitOne($TimeoutMs)) {
        throw ("RCON: connect timeout to " + $Server + ":" + $Port + " (server down or enable-rcon=false)")
    }
    $client.EndConnect($iar)
    $client.ReceiveTimeout = $TimeoutMs
    $stream = $client.GetStream()

    # ---- login (type=3) ----
    $loginPacket = New-RconPacket -PacketId 1 -Type 3 -Body $Password
    $stream.Write($loginPacket, 0, $loginPacket.Length)
    $stream.Flush()
    $login = Read-RconPacket -Stream $stream
    if ($login.Id -eq -1) { throw "RCON: login failed (wrong rcon.password)" }

    # ---- command (id=1, type=2) ----
    $cmdPacket = New-RconPacket -PacketId 1 -Type 2 -Body $Command
    $stream.Write($cmdPacket, 0, $cmdPacket.Length)
    $stream.Flush()

    # response may arrive as several packets: read first, then drain with a short timeout
    $chunks = New-Object System.Collections.Generic.List[string]
    $first = Read-RconPacket -Stream $stream
    if ($first.Type -ne 0) { throw ("RCON: expected response type 0, got " + $first.Type) }
    $chunks.Add($first.Body)
    $stream.ReadTimeout = 400
    while ($true) {
        try {
            if (-not $stream.DataAvailable) {
                Start-Sleep -Milliseconds 60
                if (-not $stream.DataAvailable) { break }
            }
            $more = Read-RconPacket -Stream $stream
            if ($more.Type -eq 0) { $chunks.Add($more.Body) }
        }
        catch { break }
    }

    # Write-Output (not [Console]::Out) so callers can capture the body in a pipeline
    $text = ($chunks -join "`n")
    Write-Output $text
}
finally {
    try { $client.Close() } catch { }
}
