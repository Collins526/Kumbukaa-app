$dbHost = 'ep-frosty-tree-am7wpkc4-pooler.c-5.us-east-1.aws.neon.tech'
$port = 5432
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect($dbHost, $port)
    $stream = $tcp.GetStream()
    $ssl = New-Object System.Net.Security.SslStream($stream, $false, { param($sender,$cert,$chain,$errors) return $true })
    $ssl.AuthenticateAsClient($dbHost)
    Write-Host 'SSL handshake succeeded'
    $ssl.Close()
    $tcp.Close()
} catch {
    Write-Host 'SSL handshake failed:' $_.Exception.Message
    if ($_.Exception.InnerException) { Write-Host 'Inner:' $_.Exception.InnerException.Message }
    exit 1
}
