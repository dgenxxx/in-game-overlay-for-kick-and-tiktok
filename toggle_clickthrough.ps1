param([string]$action)

$targetTitle = "KickChatOverlay_Window"

$code = @"
using System;
using System.Runtime.InteropServices;
using System.Text;

public class Win32 {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll", EntryPoint = "GetWindowLong")]
    private static extern IntPtr GetWindowLongPtr32(IntPtr hWnd, int nIndex);

    [DllImport("user32.dll", EntryPoint = "GetWindowLongPtr")]
    private static extern IntPtr GetWindowLongPtr64(IntPtr hWnd, int nIndex);

    [DllImport("user32.dll", EntryPoint = "SetWindowLong")]
    private static extern int SetWindowLong32(IntPtr hWnd, int nIndex, int dwNewLong);

    [DllImport("user32.dll", EntryPoint = "SetWindowLongPtr")]
    private static extern IntPtr SetWindowLongPtr64(IntPtr hWnd, int nIndex, IntPtr dwNewLong);

    public static IntPtr GetWindowLongPtr(IntPtr hWnd, int nIndex) {
        if (IntPtr.Size == 8) return GetWindowLongPtr64(hWnd, nIndex);
        else return GetWindowLongPtr32(hWnd, nIndex);
    }

    public static IntPtr SetWindowLongPtr(IntPtr hWnd, int nIndex, IntPtr dwNewLong) {
        if (IntPtr.Size == 8) return SetWindowLongPtr64(hWnd, nIndex, dwNewLong);
        else return (IntPtr)SetWindowLong32(hWnd, nIndex, (int)dwNewLong.ToInt64());
    }

    public const int GWL_EXSTYLE = -20;
    public const long WS_EX_LAYERED = 0x80000;
    public const long WS_EX_TRANSPARENT = 0x20;
}
"@

Add-Type -TypeDefinition $code -Language CSharp

$foundHwnd = [IntPtr]::Zero

$proc = {
    param($hwnd, $lparam)
    $sb = New-Object System.Text.StringBuilder(256)
    [Win32]::GetWindowText($hwnd, $sb, 256)
    $title = $sb.ToString()
    
    # Debug: Uncomment to see all windows
    # if ($title.Length -gt 0) { Write-Host "Checking: $title" }

    if ($title -eq $Global:targetTitle) {
        $Global:foundHwnd = $hwnd
        return $false # Stop enumeration
    }
    return $true
}

[Win32]::EnumWindows($proc, [IntPtr]::Zero)

if ($Global:foundHwnd -eq [IntPtr]::Zero) {
    Write-Host "Window '$targetTitle' not found via EnumWindows."
    # Fallback debug: list windows with 'Kick'
    Write-Host "Listing windows containing 'Kick':"
    $debugProc = {
        param($hwnd, $lparam)
        $sb = New-Object System.Text.StringBuilder(256)
        [Win32]::GetWindowText($hwnd, $sb, 256)
        $title = $sb.ToString()
        if ($title -like "*Kick*") {
            Write-Host " - '$title'"
        }
        return $true
    }
    [Win32]::EnumWindows($debugProc, [IntPtr]::Zero)
    exit 1
}

Write-Host "Found Window Handle: $Global:foundHwnd"

$stylePtr = [Win32]::GetWindowLongPtr($Global:foundHwnd, [Win32]::GWL_EXSTYLE)
$style = $stylePtr.ToInt64()

if ($action -eq "lock") {
    $newStyle = $style -bor [Win32]::WS_EX_TRANSPARENT -bor [Win32]::WS_EX_LAYERED
    [Win32]::SetWindowLongPtr($Global:foundHwnd, [Win32]::GWL_EXSTYLE, [IntPtr]$newStyle)
    Write-Host "Locked (Click-through enabled). New Style: $newStyle"
} elseif ($action -eq "unlock") {
    $newStyle = $style -band (-bnot [Win32]::WS_EX_TRANSPARENT)
    [Win32]::SetWindowLongPtr($Global:foundHwnd, [Win32]::GWL_EXSTYLE, [IntPtr]$newStyle)
    Write-Host "Unlocked (Click-through disabled). New Style: $newStyle"
}
