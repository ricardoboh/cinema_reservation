param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
$checks = [System.Collections.Generic.List[object]]::new()
function Request {
    param([string]$Method, [string]$Path, [int]$Expected = 200, [object]$Body = $null, [int]$Caller = 1)
    $parameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true; Headers = @{ 'X-User-Id' = "$Caller" } }
    if ($null -ne $Body) { $parameters.ContentType = 'application/json'; $parameters.Body = $Body | ConvertTo-Json -Compress }
    try { $response = Invoke-WebRequest @parameters; $status = [int]$response.StatusCode; $content = $response.Content }
    catch {
        if (-not $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        $content = $_.ErrorDetails.Message
        if (-not $content) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
    }
    if ($status -ne $Expected) { throw "$Method $Path expected $Expected, got $status : $content" }
    $checks.Add([pscustomobject]@{ Method=$Method; Path=$Path; Status=$status; Response=$content })
    return ($content | ConvertFrom-Json)
}
function Draft([int[]]$SeatIds) {
    $created = Request POST /reservations 201 @{ userId=1; screeningId=1; seatIds=$SeatIds }
    $state = Request GET "/reservations/$($created.id)"
    if ($state.status -ne 'DRAFT') { throw 'Create did not produce DRAFT' }
    return $created.id
}
function State([object]$Response, [string]$Expected) {
    if ($Response.status -ne $Expected) { throw "Expected $Expected, got $($Response.status)" }
}
function Availability([int[]]$SeatIds, [string]$Expected) {
    $availability = Request GET /screenings/1/availability
    foreach ($seatId in $SeatIds) {
        if (($availability | Where-Object seatId -eq $seatId).availability -ne $Expected) { throw "Seat $seatId expected $Expected" }
    }
}
$swagger = Invoke-WebRequest -UseBasicParsing "$BaseUrl/swagger-ui/index.html"
if ($swagger.StatusCode -ne 200 -or -not $swagger.Content.Contains('Swagger UI')) { throw 'Swagger UI failed' }
$api = Invoke-RestMethod "$BaseUrl/v3/api-docs"
foreach ($path in @('/reservations', '/reservations/{id}', '/reservations/{id}/approval', '/reservations/{id}/confirm', '/reservations/{id}/cancel', '/screenings/{screeningId}/availability')) {
    if (-not $api.paths.PSObject.Properties[$path]) { throw "Missing OpenAPI path: $path" }
}
# A: direct confirmation; drafts remain non-blocking.
$a = Draft @(1,2)
Availability @(1,2) AVAILABLE
State (Request POST "/reservations/$a/confirm") CONFIRMED
Availability @(1,2) UNAVAILABLE
State (Request POST "/reservations/$a/cancel") CANCELLED
# B: whole mixed reservation pending, independent authority, hold conversion.
$b = Draft @(1,3)
State (Request POST "/reservations/$b/confirm") PENDING_APPROVAL
Availability @(1,3) UNAVAILABLE
Request POST "/reservations/$b/approval" 403 @{decision='APPROVE'} 1 | Out-Null
Request POST "/reservations/$b/approval" 403 @{decision='REJECT'} 1 | Out-Null
State (Request GET "/reservations/$b") PENDING_APPROVAL
State (Request POST "/reservations/$b/approval" 200 @{decision='APPROVE'} 3) CONFIRMED
Availability @(1,3) UNAVAILABLE
State (Request POST "/reservations/$b/cancel") CANCELLED
# C: rejected request retains outcome and releases every seat.
$c = Draft @(1,3)
State (Request POST "/reservations/$c/confirm") PENDING_APPROVAL
State (Request POST "/reservations/$c/approval" 200 @{decision='REJECT'} 3) REJECTED
Availability @(1,3) AVAILABLE
Request POST "/reservations/$c/approval" 409 @{decision='APPROVE'} 3 | Out-Null
# D: owner withdrawal while pending.
$d = Draft @(1,3)
State (Request POST "/reservations/$d/confirm") PENDING_APPROVAL
State (Request POST "/reservations/$d/cancel") CANCELLED
Availability @(1,3) AVAILABLE
Request POST "/reservations/$d/approval" 409 @{decision='APPROVE'} 3 | Out-Null
# E and F: actual deadline, and conflicting Confirm while the hold is live.
$e = Draft @(1,3)
$pending = Request POST "/reservations/$e/confirm"
State $pending PENDING_APPROVAL
$f = Draft @(1,3)
Request POST "/reservations/$f/confirm" 409 | Out-Null
State (Request GET "/reservations/$f") DRAFT
Availability @(1,3) UNAVAILABLE
Write-Host "A-D and F passed. Waiting for real backend deadline $($pending.approvalDeadline)."
$deadline = [DateTimeOffset]::Parse($pending.approvalDeadline)
while ([DateTimeOffset]::UtcNow -lt $deadline.AddMilliseconds(100)) { Start-Sleep -Milliseconds 250 }
State (Request GET "/reservations/$e") EXPIRED
Availability @(1,3) AVAILABLE
Request POST "/reservations/$e/approval" 409 @{decision='APPROVE'} 3 | Out-Null
Request POST "/reservations/$e/cancel" 409 | Out-Null
State (Request POST "/reservations/$f/confirm") PENDING_APPROVAL
Request POST "/reservations/$e/approval" 409 @{decision='APPROVE'} 3 | Out-Null
State (Request POST "/reservations/$f/cancel") CANCELLED
[pscustomobject]@{ SwaggerStatus=$swagger.StatusCode; OpenApiVersion=$api.openapi; Flows='A-F PASS'; ReservationIds=@{A=$a;B=$b;C=$c;D=$d;E=$e;F=$f}; Checks=$checks }
