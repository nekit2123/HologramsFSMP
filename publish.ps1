<#
publish.ps1 — скрипт для помощи в создании репозитория на GitHub и пуше от имени пользователя.

Требования:
- PowerShell (Windows)
- git установлен и доступен в PATH
- (опционально) GitHub CLI `gh` для автоматического создания репозитория
#>

param(
    [string]$RepoName = "HologramsFSMP",
    [string]$RemoteUrl = "",
    [string]$Description = "Paper plugin: HologramsFSMP",
    [switch]$Private
)

Push-Location -Path $PSScriptRoot
try {
    if (-not (Test-Path .git)) {
        Write-Host "Инициализация локального git-репозитория..."
        git init
    }

    Write-Host "Добавляю файлы и выполняю коммит..."
    git add .
    git commit -m "Initial commit" -q || Write-Host "Коммит не создан (возможно, нет изменений)."

    if ($RemoteUrl -ne "") {
        Write-Host "Добавляю удалённый origin: $RemoteUrl"
        git remote remove origin 2>$null
        git remote add origin $RemoteUrl
    }

    if ($RemoteUrl -eq "") {
        if (Get-Command gh -ErrorAction SilentlyContinue) {
            Write-Host "Создаю репозиторий на GitHub через gh..."
            $ghArgs = @("repo", "create", $RepoName, "--description", $Description, "--source=.")
            if ($Private) { $ghArgs += "--private" }
            $ghArgs += "--push"
            gh @ghArgs
            Write-Host "Репозиторий создан и код запушен."
        }
        else {
            Write-Host "GitHub CLI (gh) не найдена. Укажи параметр -RemoteUrl или установи gh."
            Write-Host "Пример ручных команд для создания репозитория и пуша:"
            Write-Host "    git remote add origin https://github.com/USERNAME/REPO.git"
            Write-Host "    git branch -M main"
            Write-Host "    git push -u origin main"
        }
    }
    else {
        Write-Host "Устанавливаю ветку main и пушу на origin..."
        git branch -M main
        git push -u origin main
    }
}
finally {
    Pop-Location
}
