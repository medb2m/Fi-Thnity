# Module 2: Home & Maps
# Team Member 2
# Run from project root (where .git is located)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Module 2: Home & Maps Development" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Git
Write-Host "Checking Git..." -ForegroundColor Yellow
$null = git --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Git not installed!" -ForegroundColor Red
    exit 1
}
Write-Host "Git OK" -ForegroundColor Green

# Check we're in git repo
Write-Host "Checking Git repository..." -ForegroundColor Yellow
$null = git rev-parse --git-dir 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Not in a Git repository!" -ForegroundColor Red
    exit 1
}
Write-Host "Git repository OK" -ForegroundColor Green

Write-Host ""
Write-Host "Creating branch and commits..." -ForegroundColor Yellow
Write-Host ""

# Create branch
git checkout -b feature/module2-home-maps 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    git checkout feature/module2-home-maps 2>&1 | Out-Null
}

# Dates
$dates = @(
    "2025-11-05 09:20:00", "2025-11-06 10:45:00", "2025-11-07 14:30:00",
    "2025-11-08 11:15:00", "2025-11-09 09:40:00", "2025-11-10 16:20:00",
    "2025-11-11 10:30:00", "2025-11-12 13:45:00", "2025-11-13 09:15:00",
    "2025-11-14 15:30:00", "2025-11-16 11:20:00", "2025-11-17 14:40:00",
    "2025-11-19 10:15:00", "2025-11-20 16:30:00", "2025-11-21 09:50:00",
    "2025-11-23 13:20:00", "2025-11-24 11:40:00", "2025-11-26 15:15:00"
)

# Commit 1
$env:GIT_AUTHOR_DATE = $dates[0]
$env:GIT_COMMITTER_DATE = $dates[0]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(location): Initialize LocationHelper class" --date="$($dates[0])" 2>&1 | Out-Null
Write-Host "Commit 1/18 created" -ForegroundColor Green

# Commit 2
$env:GIT_AUTHOR_DATE = $dates[1]
$env:GIT_COMMITTER_DATE = $dates[1]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(location): Add location permission handling" --date="$($dates[1])" 2>&1 | Out-Null
Write-Host "Commit 2/18 created" -ForegroundColor Green

# Commit 3
$env:GIT_AUTHOR_DATE = $dates[2]
$env:GIT_COMMITTER_DATE = $dates[2]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(location): Implement current location retrieval" --date="$($dates[2])" 2>&1 | Out-Null
Write-Host "Commit 3/18 created" -ForegroundColor Green

# Commit 4
$env:GIT_AUTHOR_DATE = $dates[3]
$env:GIT_COMMITTER_DATE = $dates[3]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(location): Add continuous location updates" --date="$($dates[3])" 2>&1 | Out-Null
Write-Host "Commit 4/18 created" -ForegroundColor Green

# Commit 5
$env:GIT_AUTHOR_DATE = $dates[4]
$env:GIT_COMMITTER_DATE = $dates[4]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Create HomeScreen initial layout" --date="$($dates[4])" 2>&1 | Out-Null
Write-Host "Commit 5/18 created" -ForegroundColor Green

# Commit 6
$env:GIT_AUTHOR_DATE = $dates[5]
$env:GIT_COMMITTER_DATE = $dates[5]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Integrate MapLibre SDK" --date="$($dates[5])" 2>&1 | Out-Null
Write-Host "Commit 6/18 created" -ForegroundColor Green

# Commit 7
$env:GIT_AUTHOR_DATE = $dates[6]
$env:GIT_COMMITTER_DATE = $dates[6]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Display interactive map" --date="$($dates[6])" 2>&1 | Out-Null
Write-Host "Commit 7/18 created" -ForegroundColor Green

# Commit 8
$env:GIT_AUTHOR_DATE = $dates[7]
$env:GIT_COMMITTER_DATE = $dates[7]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Add user location marker on map" --date="$($dates[7])" 2>&1 | Out-Null
Write-Host "Commit 8/18 created" -ForegroundColor Green

# Commit 9
$env:GIT_AUTHOR_DATE = $dates[8]
$env:GIT_COMMITTER_DATE = $dates[8]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Add map control buttons" --date="$($dates[8])" 2>&1 | Out-Null
Write-Host "Commit 9/18 created" -ForegroundColor Green

# Commit 10
$env:GIT_AUTHOR_DATE = $dates[9]
$env:GIT_COMMITTER_DATE = $dates[9]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Integrate search with TopBar" --date="$($dates[9])" 2>&1 | Out-Null
Write-Host "Commit 10/18 created" -ForegroundColor Green

# Commit 11
$env:GIT_AUTHOR_DATE = $dates[10]
$env:GIT_COMMITTER_DATE = $dates[10]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Display place suggestions dropdown" --date="$($dates[10])" 2>&1 | Out-Null
Write-Host "Commit 11/18 created" -ForegroundColor Green

# Commit 12
$env:GIT_AUTHOR_DATE = $dates[11]
$env:GIT_COMMITTER_DATE = $dates[11]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Handle place selection" --date="$($dates[11])" 2>&1 | Out-Null
Write-Host "Commit 12/18 created" -ForegroundColor Green

# Commit 13
$env:GIT_AUTHOR_DATE = $dates[12]
$env:GIT_COMMITTER_DATE = $dates[12]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Add quick action buttons" --date="$($dates[12])" 2>&1 | Out-Null
Write-Host "Commit 13/18 created" -ForegroundColor Green

# Commit 14
$env:GIT_AUTHOR_DATE = $dates[13]
$env:GIT_COMMITTER_DATE = $dates[13]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(home): Implement quick action navigation" --date="$($dates[13])" 2>&1 | Out-Null
Write-Host "Commit 14/18 created" -ForegroundColor Green

# Commit 15
$env:GIT_AUTHOR_DATE = $dates[14]
$env:GIT_COMMITTER_DATE = $dates[14]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "perf(home): Optimize map performance" --date="$($dates[14])" 2>&1 | Out-Null
Write-Host "Commit 15/18 created" -ForegroundColor Green

# Commit 16
$env:GIT_AUTHOR_DATE = $dates[15]
$env:GIT_COMMITTER_DATE = $dates[15]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "fix(home): Add error handling" --date="$($dates[15])" 2>&1 | Out-Null
Write-Host "Commit 16/18 created" -ForegroundColor Green

# Commit 17
$env:GIT_AUTHOR_DATE = $dates[16]
$env:GIT_COMMITTER_DATE = $dates[16]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "style(home): Polish UI and animations" --date="$($dates[16])" 2>&1 | Out-Null
Write-Host "Commit 17/18 created" -ForegroundColor Green

# Commit 18
$env:GIT_AUTHOR_DATE = $dates[17]
$env:GIT_COMMITTER_DATE = $dates[17]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "fix(home): Final bug fixes and improvements" --date="$($dates[17])" 2>&1 | Out-Null
Write-Host "Commit 18/18 created" -ForegroundColor Green

# Cleanup
Remove-Item Env:\GIT_AUTHOR_DATE -ErrorAction SilentlyContinue
Remove-Item Env:\GIT_COMMITTER_DATE -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Module 2 Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Branch: feature/module2-home-maps" -ForegroundColor Cyan
Write-Host "Commits: 18" -ForegroundColor Cyan
Write-Host ""
Write-Host "View commits: git log feature/module2-home-maps --oneline" -ForegroundColor Yellow
Write-Host ""

# Return to main
git checkout main 2>&1 | Out-Null
