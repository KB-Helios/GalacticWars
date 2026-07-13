param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

Add-Type -AssemblyName System.Drawing

$textureRoot = Join-Path $RepositoryRoot 'src/main/resources/assets/galacticwars/textures/item/lightsaber'
$variants = [ordered]@{
    blue   = [System.Drawing.Color]::FromArgb(255, 35, 145, 255)
    green  = [System.Drawing.Color]::FromArgb(255, 45, 220, 105)
    red    = [System.Drawing.Color]::FromArgb(255, 245, 55, 55)
    purple = [System.Drawing.Color]::FromArgb(255, 165, 75, 255)
    yellow = [System.Drawing.Color]::FromArgb(255, 255, 205, 45)
    white  = [System.Drawing.Color]::FromArgb(255, 205, 235, 255)
}

function Set-PixelSafe {
    param($Bitmap, [int]$X, [int]$Y, $Color)
    if ($X -ge 0 -and $X -lt $Bitmap.Width -and $Y -ge 0 -and $Y -lt $Bitmap.Height) {
        $Bitmap.SetPixel($X, $Y, $Color)
    }
}

function New-HiltTexture {
    param([string]$Name, $Accent)
    $bitmap = [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt 32; $y++) {
            for ($x = 0; $x -lt 32; $x++) {
                $brush = (($x * 5 + $y * 3) % 7) - 3
                $base = [Math]::Clamp(82 + $brush + [int](($x / 31.0) * 28), 45, 135)
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $base, $base + 4, $base + 8))
            }
        }

        for ($y = 0; $y -le 6; $y++) {
            for ($x = 0; $x -lt 32; $x++) {
                $shine = [Math]::Clamp(128 + (($x + $y) % 5) * 8, 0, 190)
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $shine, $shine + 3, $shine + 7))
            }
        }
        for ($y = 1; $y -le 5; $y++) {
            for ($x = 12; $x -le 19; $x++) {
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 24, 28, 34))
            }
        }

        foreach ($bandY in 7, 8, 14, 15, 29, 30) {
            for ($x = 0; $x -lt 32; $x++) {
                $value = if ($bandY -in 14, 15) { 122 } else { 158 }
                $bitmap.SetPixel($x, $bandY, [System.Drawing.Color]::FromArgb(255, $value, $value + 5, $value + 9))
            }
        }

        for ($y = 17; $y -le 28; $y++) {
            for ($x = 0; $x -lt 32; $x++) {
                $rib = if ((($y - 17) % 3) -eq 0) { 18 } else { 34 + (($x + $y) % 5) }
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $rib, $rib + 2, $rib + 5))
            }
        }

        for ($y = 9; $y -le 13; $y++) {
            for ($x = 3; $x -le 7; $x++) {
                $alphaScale = if ($x -eq 5) { 1.0 } else { 0.72 }
                $r = [int]($Accent.R * $alphaScale)
                $g = [int]($Accent.G * $alphaScale)
                $b = [int]($Accent.B * $alphaScale)
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
            }
        }
        for ($y = 9; $y -le 13; $y++) {
            Set-PixelSafe $bitmap 2 $y ([System.Drawing.Color]::FromArgb(255, 28, 31, 36))
            Set-PixelSafe $bitmap 8 $y ([System.Drawing.Color]::FromArgb(255, 28, 31, 36))
        }
        $bitmap.Save((Join-Path $textureRoot ($Name + '_hilt.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $bitmap.Dispose()
    }
}

function New-BladeTexture {
    param([string]$Name, $Accent)
    $bitmap = [System.Drawing.Bitmap]::new(32, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $bitmap.MakeTransparent()
        for ($frame = 0; $frame -lt 4; $frame++) {
            $pulse = @(0.82, 0.94, 1.0, 0.9)[$frame]
            for ($y = 0; $y -lt 32; $y++) {
                $frameY = $frame * 32 + $y
                $tipFade = if ($y -lt 3) { ($y + 1) / 4.0 } else { 1.0 }
                for ($x = 3; $x -le 28; $x++) {
                    $distance = [Math]::Abs(15.5 - $x)
                    if ($distance -le 3.5) {
                        $color = [System.Drawing.Color]::FromArgb(255, 245, 252, 255)
                    }
                    elseif ($distance -le 7.5) {
                        $r = [Math]::Clamp([int]($Accent.R * $pulse * $tipFade), 0, 255)
                        $g = [Math]::Clamp([int]($Accent.G * $pulse * $tipFade), 0, 255)
                        $b = [Math]::Clamp([int]($Accent.B * $pulse * $tipFade), 0, 255)
                        $color = [System.Drawing.Color]::FromArgb(235, $r, $g, $b)
                    }
                    elseif ($distance -le 10.5) {
                        $color = [System.Drawing.Color]::FromArgb(
                                [int](118 * $tipFade), $Accent.R, $Accent.G, $Accent.B)
                    }
                    else {
                        $color = [System.Drawing.Color]::FromArgb(
                                [int](38 * $tipFade), $Accent.R, $Accent.G, $Accent.B)
                    }
                    $bitmap.SetPixel($x, $frameY, $color)
                }
            }
        }
        $bitmap.Save((Join-Path $textureRoot ($Name + '_blade.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $bitmap.Dispose()
    }
}

foreach ($entry in $variants.GetEnumerator()) {
    New-HiltTexture $entry.Key $entry.Value
    New-BladeTexture $entry.Key $entry.Value
}

Write-Host "Generated $($variants.Count) hilt textures and $($variants.Count) animated blade textures."
