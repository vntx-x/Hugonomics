Add-Type -AssemblyName System.Drawing
$n = 32
$cell = 16
$size = $n * $cell
$bmp = New-Object System.Drawing.Bitmap $size, $size
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'AntiAlias'
$g.Clear([System.Drawing.Color]::Transparent)

function RoundRect($x, $y, $w, $h, $r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddArc($x, $y, $r * 2, $r * 2, 180, 90)
    $p.AddArc($x + $w - $r * 2, $y, $r * 2, $r * 2, 270, 90)
    $p.AddArc($x + $w - $r * 2, $y + $h - $r * 2, $r * 2, $r * 2, 0, 90)
    $p.AddArc($x, $y + $h - $r * 2, $r * 2, $r * 2, 90, 90)
    $p.CloseFigure()
    return $p
}
function C($hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }

$bg = RoundRect 0 0 $size $size 80
$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush ([System.Drawing.Point]::new(0, 0)), ([System.Drawing.Point]::new($size, $size)), (C '#5a2d9c'), (C '#22103f')
$g.FillPath($grad, $bg)
$g.SetClip($bg)
$g.SmoothingMode = 'None'

$grid = New-Object 'string[,]' $n, $n
for ($y = 0; $y -lt $n; $y++) { for ($x = 0; $x -lt $n; $x++) { $grid[$x, $y] = '' } }

$cx = 13.5; $cy = 17.5; $r = 11.5
for ($y = 0; $y -lt $n; $y++) {
    for ($x = 0; $x -lt $n; $x++) {
        $dx = $x + 0.5 - $cx; $dy = $y + 0.5 - $cy
        $d = [math]::Sqrt($dx * $dx + $dy * $dy)
        if ($d -gt $r) { continue }
        if ($d -gt $r - 1.2) { $grid[$x, $y] = '#6b4208'; continue }
        if ($d -gt $r - 3.2) {
            if ($dx + $dy -lt -4) { $grid[$x, $y] = '#ffe68f' }
            elseif ($dx + $dy -gt 4) { $grid[$x, $y] = '#c47f12' }
            else { $grid[$x, $y] = '#f4b53a' }
            continue
        }
        if ($d -gt $r - 4.2) { $grid[$x, $y] = '#9c5f0d'; continue }
        $grid[$x, $y] = '#f4b53a'
    }
}

function Fill($x0, $y0, $x1, $y1, $col) {
    for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { if ($x -ge 0 -and $y -ge 0 -and $x -lt $n -and $y -lt $n) { $grid[$x, $y] = $col } } }
}

Fill 9 12 10 23 '#9c5f0d'; Fill 16 12 17 23 '#9c5f0d'; Fill 11 17 15 18 '#9c5f0d'
Fill 8 11 9 22 '#fff3c4'; Fill 15 11 16 22 '#fff3c4'; Fill 10 16 14 17 '#fff3c4'
Fill 8 11 8 22 '#ffffff'; Fill 15 11 15 22 '#ffffff'

$shaft = New-Object System.Collections.ArrayList
for ($i = 0; $i -le 9; $i++) { [void]$shaft.Add(@((18 + $i), (16 - $i))) }
foreach ($o in @(@(1, 1), @(2, 2), @(1, 2), @(2, 1))) {
    foreach ($p in $shaft) { Fill ($p[0] + $o[0]) ($p[1] + $o[1]) ($p[0] + $o[0]) ($p[1] + $o[1]) '#14532d' }
}
for ($yy = 3; $yy -le 10; $yy++) { for ($xx = 22; $xx -le 29; $xx++) { if (($xx - 22) + (10 - $yy) -ge 7) { Fill ($xx + 1) ($yy + 1) ($xx + 1) ($yy + 1) '#14532d' } } }
foreach ($o in @(@(0, 0), @(1, 0), @(0, 1), @(1, 1))) {
    foreach ($p in $shaft) { Fill ($p[0] + $o[0]) ($p[1] + $o[1]) ($p[0] + $o[0]) ($p[1] + $o[1]) '#22c55e' }
}
for ($yy = 3; $yy -le 10; $yy++) { for ($xx = 22; $xx -le 29; $xx++) { if (($xx - 22) + (10 - $yy) -ge 7) { Fill $xx $yy $xx $yy '#22c55e' } } }
foreach ($p in $shaft) { Fill $p[0] $p[1] $p[0] $p[1] '#86efac' }
for ($yy = 3; $yy -le 10; $yy++) { for ($xx = 22; $xx -le 29; $xx++) { if (($xx - 22) + (10 - $yy) -eq 7) { Fill $xx $yy $xx $yy '#86efac' } } }

for ($y = 0; $y -lt $n; $y++) {
    for ($x = 0; $x -lt $n; $x++) {
        $col = $grid[$x, $y]
        if ($col -eq '') {
            if ((($x + $y) % 2) -eq 0) { $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(14, 255, 255, 255))), $x * $cell, $y * $cell, $cell, $cell) }
            continue
        }
        $g.FillRectangle((New-Object System.Drawing.SolidBrush (C $col)), $x * $cell, $y * $cell, $cell, $cell)
    }
}
$g.ResetClip()
$g.Dispose()
$bmp.Save($args[0], [System.Drawing.Imaging.ImageFormat]::Png)
$small = New-Object System.Drawing.Bitmap 128, 128
$sg = [System.Drawing.Graphics]::FromImage($small)
$sg.InterpolationMode = 'NearestNeighbor'
$sg.PixelOffsetMode = 'Half'
$sg.DrawImage($bmp, 0, 0, 128, 128)
$sg.Dispose()
$small.Save($args[1], [System.Drawing.Imaging.ImageFormat]::Png)
$small.Dispose()
$bmp.Dispose()
