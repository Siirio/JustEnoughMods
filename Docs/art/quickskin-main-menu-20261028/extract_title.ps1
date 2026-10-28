param(
    [Parameter(Mandatory = $true)]
    [string]$Source,

    [Parameter(Mandatory = $true)]
    [string]$Output
)

Add-Type -AssemblyName System.Drawing

if (-not ('JemTitleExtractor' -as [type])) {
    Add-Type -ReferencedAssemblies 'System.Drawing.dll' -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public static class JemTitleExtractor
{
    public static void Extract(string sourcePath, string outputPath)
    {
        using (var loaded = new Bitmap(sourcePath))
        using (var source = new Bitmap(loaded.Width, loaded.Height, PixelFormat.Format32bppArgb))
        {
            using (var graphics = Graphics.FromImage(source))
            {
                graphics.DrawImageUnscaled(loaded, 0, 0);
            }

            var width = source.Width;
            var height = source.Height;
            using (var output = new Bitmap(width, height, PixelFormat.Format32bppArgb))
            {
                var rectangle = new Rectangle(0, 0, width, height);
                var sourceData = source.LockBits(rectangle, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
                var outputData = output.LockBits(rectangle, ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
                var minimumX = width;
                var minimumY = height;
                var maximumX = -1;
                var maximumY = -1;

                var sourceBytes = new byte[Math.Abs(sourceData.Stride) * height];
                var outputBytes = new byte[Math.Abs(outputData.Stride) * height];
                Marshal.Copy(sourceData.Scan0, sourceBytes, 0, sourceBytes.Length);

                for (var y = 0; y < height; y++)
                {
                    var sourceRow = y * sourceData.Stride;
                    var outputRow = y * outputData.Stride;
                    for (var x = 0; x < width; x++)
                    {
                        var topLine = x >= 80 && x <= width - 80 && y >= 70 && y <= 760;
                        var bottomLine = x >= 390 && x <= width - 390 && y >= 500 && y <= height - 55;
                        if (!topLine && !bottomLine)
                        {
                            continue;
                        }

                        var index = sourceRow + x * 4;
                        var blue = sourceBytes[index];
                        var green = sourceBytes[index + 1];
                        var red = sourceBytes[index + 2];
                        var minimum = Math.Min(red, Math.Min(green, blue));
                        var maximum = Math.Max(red, Math.Max(green, blue));
                        var chroma = maximum - minimum;
                        var whiteScore = minimum - chroma * 0.72;
                        var alpha = (int)Math.Round((whiteScore - 145.0) * 4.25);
                        alpha = Math.Max(0, Math.Min(255, alpha));
                        if (alpha == 0)
                        {
                            continue;
                        }

                        var outputIndex = outputRow + x * 4;
                        outputBytes[outputIndex] = 255;
                        outputBytes[outputIndex + 1] = 255;
                        outputBytes[outputIndex + 2] = 255;
                        outputBytes[outputIndex + 3] = (byte)alpha;
                        minimumX = Math.Min(minimumX, x);
                        minimumY = Math.Min(minimumY, y);
                        maximumX = Math.Max(maximumX, x);
                        maximumY = Math.Max(maximumY, y);
                    }
                }

                Marshal.Copy(outputBytes, 0, outputData.Scan0, outputBytes.Length);

                source.UnlockBits(sourceData);
                output.UnlockBits(outputData);

                if (maximumX < minimumX || maximumY < minimumY)
                {
                    throw new InvalidOperationException("Title extraction produced no visible pixels");
                }

                const int margin = 24;
                minimumX = Math.Max(0, minimumX - margin);
                minimumY = Math.Max(0, minimumY - margin);
                maximumX = Math.Min(width - 1, maximumX + margin);
                maximumY = Math.Min(height - 1, maximumY + margin);
                var crop = Rectangle.FromLTRB(minimumX, minimumY, maximumX + 1, maximumY + 1);
                using (var cropped = output.Clone(crop, PixelFormat.Format32bppArgb))
                {
                    cropped.Save(outputPath, ImageFormat.Png);
                }
            }
        }
    }
}
'@
}

[JemTitleExtractor]::Extract($Source, $Output)
