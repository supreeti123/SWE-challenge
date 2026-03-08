param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$RateLimitBurstCount = 140
)

$ErrorActionPreference = "Stop"

function Assert($Condition, $Message) {
    if (-not $Condition) {
        throw "ASSERT FAILED: $Message"
    }
}

function Invoke-Api(
    [string]$Method,
    [string]$Url,
    $Body = $null,
    [hashtable]$Headers = @{}
) {
    $params = @{
        UseBasicParsing = $true
        Method          = $Method
        Uri             = $Url
        Headers         = $Headers
    }

    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 8)
    }

    try {
        $resp = Invoke-WebRequest @params
        $status = [int]$resp.StatusCode
        $raw = $resp.Content
        $respHeaders = $resp.Headers
    } catch [System.Net.WebException] {
        if ($null -eq $_.Exception.Response) {
            throw
        }
        $errorResponse = $_.Exception.Response
        $status = [int]$errorResponse.StatusCode
        $respHeaders = $errorResponse.Headers

        $reader = New-Object System.IO.StreamReader($errorResponse.GetResponseStream())
        $raw = $reader.ReadToEnd()
        $reader.Dispose()
    }

    $json = $null
    try {
        if ($raw) {
            $trimmed = $raw.Trim()
            if ($trimmed.StartsWith("{") -or $trimmed.StartsWith("[")) {
                $json = $raw | ConvertFrom-Json
            }
        }
    } catch {
        # Intentionally ignore body parse errors for non-JSON endpoints.
    }

    return [pscustomobject]@{
        Status  = $status
        Json    = $json
        Raw     = $raw
        Headers = $respHeaders
    }
}

$runId = [Guid]::NewGuid().ToString().Substring(0, 8)
Write-Host "Running e2e smoke test against $BaseUrl (runId=$runId)..."

# 1) Platform endpoints
$health = Invoke-Api "GET" "$BaseUrl/actuator/health"
Assert ($health.Status -eq 200) "/actuator/health should be 200"

$readiness = Invoke-Api "GET" "$BaseUrl/actuator/health/readiness"
Assert ($readiness.Status -eq 200) "/actuator/health/readiness should be 200"

$liveness = Invoke-Api "GET" "$BaseUrl/actuator/health/liveness"
Assert ($liveness.Status -eq 200) "/actuator/health/liveness should be 200"

$metrics = Invoke-Api "GET" "$BaseUrl/actuator/metrics"
Assert ($metrics.Status -eq 200) "/actuator/metrics should be 200"

$apiDocs = Invoke-Api "GET" "$BaseUrl/api-docs"
Assert ($apiDocs.Status -eq 200) "/api-docs should be 200"

$swagger = Invoke-Api "GET" "$BaseUrl/swagger-ui.html"
Assert ($swagger.Status -eq 200 -or $swagger.Status -eq 302) "/swagger-ui.html should be 200 or 302"

# 2) Create data
$future1 = Invoke-Api "POST" "$BaseUrl/api/todos" @{
    description = "future-$runId-1"
    dueAt       = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
}
Assert ($future1.Status -eq 201) "creating future-1 should return 201"
$id1 = [int64]$future1.Json.id

$future2 = Invoke-Api "POST" "$BaseUrl/api/todos" @{
    description = "future-$runId-2"
    dueAt       = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
}
Assert ($future2.Status -eq 201) "creating future-2 should return 201"
$id2 = [int64]$future2.Json.id

$pastDue = Invoke-Api "POST" "$BaseUrl/api/todos" @{
    description = "past-$runId"
    dueAt       = (Get-Date).AddDays(-2).ToString("yyyy-MM-ddTHH:mm:ss")
}
Assert ($pastDue.Status -eq 201) "creating past-due item should return 201"
Assert ($pastDue.Json.status -eq "past due") "past-due item should have status 'past due'"
$pastId = [int64]$pastDue.Json.id

# 3) Mutations
$updated = Invoke-Api "PATCH" "$BaseUrl/api/todos/$id1/description" @{ description = "future-$runId-1-updated" }
Assert ($updated.Status -eq 200) "updating description should return 200"
Assert ($updated.Json.description -eq "future-$runId-1-updated") "description should be updated"

$markedDone = Invoke-Api "PATCH" "$BaseUrl/api/todos/$id2/done"
Assert ($markedDone.Status -eq 200) "mark done should return 200"
Assert ($markedDone.Json.status -eq "done") "status should be done"
Assert ($null -ne $markedDone.Json.doneAt) "doneAt should be present when marked done"

$markedNotDone = Invoke-Api "PATCH" "$BaseUrl/api/todos/$id2/not-done"
Assert ($markedNotDone.Status -eq 200) "mark not-done should return 200"
Assert ($markedNotDone.Json.status -eq "not done") "status should be not done"
Assert ($null -eq $markedNotDone.Json.doneAt) "doneAt should be null when marked not-done"

$forbidden = Invoke-Api "PATCH" "$BaseUrl/api/todos/$pastId/description" @{ description = "forbidden-update" }
Assert ($forbidden.Status -eq 422) "past-due updates should return 422"
Assert ($forbidden.Json.code -eq "PAST_DUE_MODIFICATION_FORBIDDEN") "past-due code mismatch"

# 4) Query behavior and pagination
$defaultList = Invoke-Api "GET" "$BaseUrl/api/todos?page=0&size=100"
Assert ($defaultList.Status -eq 200) "default list should return 200"
$defaultDescriptions = @($defaultList.Json.content | ForEach-Object { $_.description })
Assert ($defaultDescriptions -contains "future-$runId-1-updated") "default list should include updated not-done item"
Assert ($defaultDescriptions -contains "future-$runId-2") "default list should include reverted not-done item"
Assert (-not ($defaultDescriptions -contains "past-$runId")) "default list should exclude past-due item"

$allList = Invoke-Api "GET" "$BaseUrl/api/todos?includeAll=true&page=0&size=100"
Assert ($allList.Status -eq 200) "includeAll list should return 200"
$allDescriptions = @($allList.Json.content | ForEach-Object { $_.description })
Assert ($allDescriptions -contains "past-$runId") "includeAll should include past-due item"

$sizeClamp = Invoke-Api "GET" "$BaseUrl/api/todos?size=500"
Assert ($sizeClamp.Status -eq 200) "size clamp list should return 200"
Assert ([int]$sizeClamp.Json.size -eq 100) "size should be clamped to 100"

# 5) Error contract
$invalidId = Invoke-Api "GET" "$BaseUrl/api/todos/-1"
Assert ($invalidId.Status -eq 400) "negative id should return 400"
Assert ($invalidId.Json.code -eq "VALIDATION_FAILED") "negative id code mismatch"

$missing = Invoke-Api "GET" "$BaseUrl/api/todos/999999999"
Assert ($missing.Status -eq 404) "missing item should return 404"
Assert ($missing.Json.code -eq "TODO_NOT_FOUND") "missing item code mismatch"
Assert ($null -ne $missing.Json.requestId) "error response should include requestId"

# 6) Request-id propagation
$customRequestId = "manual-$runId"
$withRequestId = Invoke-Api "GET" "$BaseUrl/api/todos/$id1" $null @{ "X-Request-Id" = $customRequestId }
Assert ($withRequestId.Status -eq 200) "item fetch with request-id should return 200"
Assert ($withRequestId.Headers["X-Request-Id"] -eq $customRequestId) "X-Request-Id should be echoed"

# 7) Rate-limit behavior (best effort, hard assert if reached)
$hitRateLimit = $false
for ($i = 0; $i -lt $RateLimitBurstCount; $i++) {
    $burst = Invoke-Api "POST" "$BaseUrl/api/todos" @{
        description = "rl-$runId-$i"
        dueAt       = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
    }

    if ($burst.Status -eq 429) {
        Assert ($burst.Json.code -eq "RATE_LIMIT_EXCEEDED") "429 code should be RATE_LIMIT_EXCEEDED"
        Assert ($null -ne $burst.Headers["Retry-After"]) "429 should include Retry-After header"
        $hitRateLimit = $true
        break
    }
}
Assert $hitRateLimit "expected to hit write rate limit during burst writes"

Write-Host "ALL_E2E_SMOKE_TESTS_PASSED runId=$runId"
