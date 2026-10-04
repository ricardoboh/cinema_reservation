param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
# Run against a fresh dev instance. This script creates and transitions demo data.
function Check-Request {
    param([string]$Method, [string]$Path, [int]$Expected, [string]$Body = '', [int]$Caller = 1)
    $parameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true }
    if ($Method -eq 'POST') { $parameters.Headers = @{ 'X-User-Id' = "$Caller" } }
    if ($Body) { $parameters.ContentType = 'application/json'; $parameters.Body = $Body }
    try {
        $response = Invoke-WebRequest @parameters
        $status = [int]$response.StatusCode
        $content = $response.Content
    } catch {
        if (-not $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        $content = $_.ErrorDetails.Message
        if (-not $content) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
    }
    if ($status -ne $Expected) { throw "$Method $Path expected $Expected, got $status : $content" }
    [pscustomobject]@{ Method = $Method; Path = $Path; Status = $status; Response = $content }
}

$ui = Invoke-WebRequest -UseBasicParsing "$BaseUrl/swagger-ui/index.html"
if ($ui.StatusCode -ne 200 -or -not $ui.Content.Contains('Swagger UI')) { throw 'Swagger UI failed' }
$api = Invoke-RestMethod "$BaseUrl/v3/api-docs"
foreach ($path in @('/reservations', '/screenings/{screeningId}/availability', '/reservations/{id}/confirm', '/reservations/{id}/cancel')) {
    if (-not $api.paths.PSObject.Properties[$path]) { throw "Missing OpenAPI path: $path" }
}
Write-Host "Swagger UI: 200; OpenAPI: $($api.openapi); all four paths present"

Check-Request GET /screenings/1/availability 200
Check-Request GET /screenings/999999/availability 404
$a = Check-Request POST /reservations 201 '{"userId":1,"screeningId":1,"seatIds":[1,2]}'
$a
$aId = ($a.Response | ConvertFrom-Json).id
$b = Check-Request POST /reservations 201 '{"userId":1,"screeningId":1,"seatIds":[1,2]}'
$b
$bId = ($b.Response | ConvertFrom-Json).id
Check-Request GET /screenings/1/availability 200
Check-Request POST /reservations 409 '{"userId":1,"screeningId":2,"seatIds":[1]}'
Check-Request POST "/reservations/$aId/confirm" 200
Check-Request GET /screenings/1/availability 200
Check-Request POST "/reservations/$bId/confirm" 409
Check-Request POST /reservations 201 '{"userId":1,"screeningId":1,"seatIds":[1,2]}'
Check-Request POST "/reservations/$aId/cancel" 200
Check-Request GET /screenings/1/availability 200
Check-Request POST "/reservations/$aId/cancel" 409
Check-Request POST "/reservations/$aId/confirm" 409
Check-Request POST "/reservations/$bId/confirm" 403 '' 2
Check-Request POST /reservations/100/confirm 409
Check-Request POST /reservations/101/cancel 409
Check-Request POST "/reservations/$bId/cancel" 200
