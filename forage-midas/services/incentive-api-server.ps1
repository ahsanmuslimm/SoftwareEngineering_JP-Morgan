# Simple PowerShell HTTP server to simulate Incentive API
# Usage: powershell -ExecutionPolicy Bypass -File incentive-api-server.ps1

Write-Host "Starting Incentive API Server on http://localhost:8080"
Write-Host "Endpoint: POST /incentive"
Write-Host "Press Ctrl+C to stop the server"
Write-Host ""

# Create HTTP listener
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:8080/")
$listener.Start()

try {
    while ($listener.IsListening) {
        # Wait for a request
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        
        # Handle POST /incentive
        if ($request.HttpMethod -eq "POST" -and $request.Url.AbsolutePath -eq "/incentive") {
            # Read request body (Transaction JSON)
            $reader = New-Object System.IO.StreamReader($request.InputStream)
            $requestBody = $reader.ReadToEnd()
            $reader.Close()
            
            Write-Host "Received POST /incentive request: $requestBody"
            
            # Return fixed incentive amount
            $responseJson = '{"amount": 15.0}'
            $responseBytes = [System.Text.Encoding]::UTF8.GetBytes($responseJson)
            
            $response.ContentType = "application/json"
            $response.ContentLength64 = $responseBytes.Length
            $response.StatusCode = 200
            
            $response.OutputStream.Write($responseBytes, 0, $responseBytes.Length)
            $response.OutputStream.Close()
            
            Write-Host "Returned: $responseJson"
        }
        else {
            # Return 404 for other requests
            $response.StatusCode = 404
            $response.Close()
        }
    }
}
catch {
    Write-Host "Server error: $_"
}
finally {
    $listener.Stop()
    Write-Host "Server stopped."
}