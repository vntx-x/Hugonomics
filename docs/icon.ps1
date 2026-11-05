Add-Type -AssemblyName System.Drawing
$S = 512
function C($hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Brush($hex) { return New-Object System.Drawing.SolidBrush (C $hex) }
function RoundRect($x, $y, $w, $h, $r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddArc($x, $y, $r * 2, $r * 2, 180, 90)
    $p.AddArc($x + $w - $r * 2, $y, $r * 2, $r * 2, 270, 90)
    $p.AddArc($x + $w - $r * 2, $y + $h - $r * 2, $r * 2, $r * 2, 0, 90)
    $p.AddArc($x, $y + $h - $r * 2, $r * 2, $r * 2, 90, 90)
    $p.CloseFigure()
    return $p
}
$bmp = New-Object System.Drawing.Bitmap $S, $S
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'AntiAlias'; $g.PixelOffsetMode = 'HighQuality'
$g.Clear([System.Drawing.Color]::Transparent)
$bg = RoundRect 0 0 $S $S 100
$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush ([System.Drawing.Point]::new(0, 0)), ([System.Drawing.Point]::new($S, $S)), (C '#7c3aed'), (C '#3b0f7a')
$g.FillPath($grad, $bg)
$g.SetClip($bg)

$g.FillEllipse((Brush '#2c0f5c'), 84, 106, 352, 352)
$g.FillEllipse((Brush '#ffcb3c'), 80, 80, 352, 352)
$g.FillEllipse((Brush '#e89d00'), 118, 118, 276, 276)
$g.FillEllipse((Brush '#ffcb3c'), 134, 134, 244, 244)

$ink = '#2c0f5c'
$g.FillPath((Brush $ink), (RoundRect 176 176 44 160 12))
$g.FillPath((Brush $ink), (RoundRect 292 176 44 160 12))
$g.FillPath((Brush $ink), (RoundRect 206 236 100 40 10))

$pen = New-Object System.Drawing.Pen (C '#22c55e'), 26
$pen.StartCap = 'Round'; $pen.EndCap = 'Round'; $pen.LineJoin = 'Round'
$g.DrawLines($pen, [System.Drawing.PointF[]]@([System.Drawing.PointF]::new(330, 128), [System.Drawing.PointF]::new(392, 128), [System.Drawing.PointF]::new(392, 190)))
$g.DrawLine($pen, 392, 128, 318, 202)

$g.ResetClip(); $g.Dispose()
$bmp.Save($args[0], [System.Drawing.Imaging.ImageFormat]::Png)
$small = New-Object System.Drawing.Bitmap $bmp, 128, 128
$small.Save($args[1], [System.Drawing.Imaging.ImageFormat]::Png)
$small.Dispose(); $bmp.Dispose()
