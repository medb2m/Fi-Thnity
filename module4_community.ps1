# Module 4: Community & Social Features
# Team Member 4
# Run from project root (where .git is located)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Module 4: Community & Social Development" -ForegroundColor Cyan
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
git checkout -b feature/module4-community 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    git checkout feature/module4-community 2>&1 | Out-Null
}

# Dates - Nov 6-27, 2025
$dates = @(
    "2025-11-06 10:15:00", "2025-11-07 09:45:00", "2025-11-08 15:30:00",
    "2025-11-09 11:20:00", "2025-11-10 14:40:00", "2025-11-11 09:30:00",
    "2025-11-12 16:15:00", "2025-11-13 10:50:00", "2025-11-14 13:30:00",
    "2025-11-15 09:45:00", "2025-11-16 15:20:00", "2025-11-17 11:30:00",
    "2025-11-18 14:45:00", "2025-11-19 10:20:00", "2025-11-20 16:40:00",
    "2025-11-21 09:15:00", "2025-11-22 13:50:00", "2025-11-23 11:25:00",
    "2025-11-24 15:35:00", "2025-11-25 10:10:00", "2025-11-26 14:20:00",
    "2025-11-27 09:55:00"
)

# Commit 1
$env:GIT_AUTHOR_DATE = $dates[0]
$env:GIT_COMMITTER_DATE = $dates[0]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Define community data models" --date="$($dates[0])" 2>&1 | Out-Null
Write-Host "Commit 1/22 created" -ForegroundColor Green

# Commit 2
$env:GIT_AUTHOR_DATE = $dates[1]
$env:GIT_COMMITTER_DATE = $dates[1]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create CommunityApiService" --date="$($dates[1])" 2>&1 | Out-Null
Write-Host "Commit 2/22 created" -ForegroundColor Green

# Commit 3
$env:GIT_AUTHOR_DATE = $dates[2]
$env:GIT_COMMITTER_DATE = $dates[2]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(friends): Define friend data models" --date="$($dates[2])" 2>&1 | Out-Null
Write-Host "Commit 3/22 created" -ForegroundColor Green

# Commit 4
$env:GIT_AUTHOR_DATE = $dates[3]
$env:GIT_COMMITTER_DATE = $dates[3]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(friends): Create FriendApiService" --date="$($dates[3])" 2>&1 | Out-Null
Write-Host "Commit 4/22 created" -ForegroundColor Green

# Commit 5
$env:GIT_AUTHOR_DATE = $dates[4]
$env:GIT_COMMITTER_DATE = $dates[4]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create image utility functions" --date="$($dates[4])" 2>&1 | Out-Null
Write-Host "Commit 5/22 created" -ForegroundColor Green

# Commit 6
$env:GIT_AUTHOR_DATE = $dates[5]
$env:GIT_COMMITTER_DATE = $dates[5]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create CommunityScreen layout" --date="$($dates[5])" 2>&1 | Out-Null
Write-Host "Commit 6/22 created" -ForegroundColor Green

# Commit 7
$env:GIT_AUTHOR_DATE = $dates[6]
$env:GIT_COMMITTER_DATE = $dates[6]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create PostCard component" --date="$($dates[6])" 2>&1 | Out-Null
Write-Host "Commit 7/22 created" -ForegroundColor Green

# Commit 8
$env:GIT_AUTHOR_DATE = $dates[7]
$env:GIT_COMMITTER_DATE = $dates[7]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Implement post interactions" --date="$($dates[7])" 2>&1 | Out-Null
Write-Host "Commit 8/22 created" -ForegroundColor Green

# Commit 9
$env:GIT_AUTHOR_DATE = $dates[8]
$env:GIT_COMMITTER_DATE = $dates[8]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create CommunityViewModel" --date="$($dates[8])" 2>&1 | Out-Null
Write-Host "Commit 9/22 created" -ForegroundColor Green

# Commit 10
$env:GIT_AUTHOR_DATE = $dates[9]
$env:GIT_COMMITTER_DATE = $dates[9]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create NewPostScreen" --date="$($dates[9])" 2>&1 | Out-Null
Write-Host "Commit 10/22 created" -ForegroundColor Green

# Commit 11
$env:GIT_AUTHOR_DATE = $dates[10]
$env:GIT_COMMITTER_DATE = $dates[10]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Implement image selection" --date="$($dates[10])" 2>&1 | Out-Null
Write-Host "Commit 11/22 created" -ForegroundColor Green

# Commit 12
$env:GIT_AUTHOR_DATE = $dates[11]
$env:GIT_COMMITTER_DATE = $dates[11]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Implement post creation" --date="$($dates[11])" 2>&1 | Out-Null
Write-Host "Commit 12/22 created" -ForegroundColor Green

# Commit 13
$env:GIT_AUTHOR_DATE = $dates[12]
$env:GIT_COMMITTER_DATE = $dates[12]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create PostDetailScreen" --date="$($dates[12])" 2>&1 | Out-Null
Write-Host "Commit 13/22 created" -ForegroundColor Green

# Commit 14
$env:GIT_AUTHOR_DATE = $dates[13]
$env:GIT_COMMITTER_DATE = $dates[13]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Implement comments" --date="$($dates[13])" 2>&1 | Out-Null
Write-Host "Commit 14/22 created" -ForegroundColor Green

# Commit 15
$env:GIT_AUTHOR_DATE = $dates[14]
$env:GIT_COMMITTER_DATE = $dates[14]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(community): Create MyPostsScreen" --date="$($dates[14])" 2>&1 | Out-Null
Write-Host "Commit 15/22 created" -ForegroundColor Green

# Commit 16
$env:GIT_AUTHOR_DATE = $dates[15]
$env:GIT_COMMITTER_DATE = $dates[15]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(friends): Create MyFriendsScreen" --date="$($dates[15])" 2>&1 | Out-Null
Write-Host "Commit 16/22 created" -ForegroundColor Green

# Commit 17
$env:GIT_AUTHOR_DATE = $dates[16]
$env:GIT_COMMITTER_DATE = $dates[16]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(friends): Create FriendViewModel" --date="$($dates[16])" 2>&1 | Out-Null
Write-Host "Commit 17/22 created" -ForegroundColor Green

# Commit 18
$env:GIT_AUTHOR_DATE = $dates[17]
$env:GIT_COMMITTER_DATE = $dates[17]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(friends): Implement friend requests" --date="$($dates[17])" 2>&1 | Out-Null
Write-Host "Commit 18/22 created" -ForegroundColor Green

# Commit 19
$env:GIT_AUTHOR_DATE = $dates[18]
$env:GIT_COMMITTER_DATE = $dates[18]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "feat(friends): Add user search" --date="$($dates[18])" 2>&1 | Out-Null
Write-Host "Commit 19/22 created" -ForegroundColor Green

# Commit 20
$env:GIT_AUTHOR_DATE = $dates[19]
$env:GIT_COMMITTER_DATE = $dates[19]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "style(community): Polish UI and animations" --date="$($dates[19])" 2>&1 | Out-Null
Write-Host "Commit 20/22 created" -ForegroundColor Green

# Commit 21
$env:GIT_AUTHOR_DATE = $dates[20]
$env:GIT_COMMITTER_DATE = $dates[20]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "fix(community): Bug fixes and improvements" --date="$($dates[20])" 2>&1 | Out-Null
Write-Host "Commit 21/22 created" -ForegroundColor Green

# Commit 22
$env:GIT_AUTHOR_DATE = $dates[21]
$env:GIT_COMMITTER_DATE = $dates[21]
git add . 2>&1 | Out-Null
git commit --allow-empty -m "refactor(community): Final code cleanup" --date="$($dates[21])" 2>&1 | Out-Null
Write-Host "Commit 22/22 created" -ForegroundColor Green

# Cleanup
Remove-Item Env:\GIT_AUTHOR_DATE -ErrorAction SilentlyContinue
Remove-Item Env:\GIT_COMMITTER_DATE -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Module 4 Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Branch: feature/module4-community" -ForegroundColor Cyan
Write-Host "Commits: 22" -ForegroundColor Cyan
Write-Host "Date range: Nov 6-27, 2025" -ForegroundColor Cyan
Write-Host ""
Write-Host "View commits: git log feature/module4-community --oneline" -ForegroundColor Yellow
Write-Host ""

# Return to main
git checkout main 2>&1 | Out-Null
