$root='c:\Projects\EscrowX\Application\EscrowX\app\src\main\java\mobile\project\escrowx'
$files=Get-ChildItem -Path $root -Recurse -Filter *.kt | Where-Object { Select-String -Path $_.FullName -Pattern 'param\(\$m\)' -Quiet }
$commonImports=@(
'import android.*',
'import android.content.*',
'import android.os.*',
'import android.widget.*',
'import androidx.activity.*',
'import androidx.activity.compose.*',
'import androidx.activity.result.contract.ActivityResultContracts',
'import androidx.appcompat.app.*',
'import androidx.compose.foundation.*',
'import androidx.compose.foundation.layout.*',
'import androidx.compose.foundation.lazy.*',
'import androidx.compose.foundation.shape.*',
'import androidx.compose.material.icons.*',
'import androidx.compose.material.icons.automirrored.filled.*',
'import androidx.compose.material.icons.filled.*',
'import androidx.compose.material3.*',
'import androidx.compose.runtime.*',
'import androidx.compose.ui.*',
'import androidx.compose.ui.draw.*',
'import androidx.compose.ui.graphics.*',
'import androidx.compose.ui.graphics.vector.*',
'import androidx.compose.ui.layout.*',
'import androidx.compose.ui.platform.*',
'import androidx.compose.ui.text.font.*',
'import androidx.compose.ui.text.style.*',
'import androidx.compose.ui.tooling.preview.*',
'import androidx.compose.ui.unit.*',
'import androidx.core.content.*',
'import androidx.core.content.FileProvider',
'import androidx.lifecycle.*',
'import androidx.lifecycle.viewmodel.compose.*',
'import kotlinx.coroutines.*',
'import java.io.*',
'import java.text.*',
'import java.util.*',
'import mobile.project.escrowx.*',
'import mobile.project.escrowx.auth.*',
'import mobile.project.escrowx.dash.*',
'import mobile.project.escrowx.seller.*',
'import mobile.project.escrowx.ui.components.*',
'import mobile.project.escrowx.ui.theme.*'
)

foreach($f in $files){
  $text=Get-Content -Raw $f.FullName
  $lines=$text -split "`r?`n"
  $cleanLines=@()
  foreach($line in $lines){
    $trim=$line.Trim()
    if($trim -match '^param\(\$m\)$' -or
       $trim -match '^\$pkg=\$m\.Groups\[1\]\.Value$' -or
       $trim -match '^\$imports=\$m\.Groups\[2\]\.Value$' -or
       $trim -match '^\$decl=\$m\.Groups\[3\]\.Value$' -or
       $trim -match '^\$importsTrim=\$imports\.TrimEnd\(\)$' -or
       $trim -match '^if\(\$importsTrim\.Length -gt 0\)\{$' -or
       $trim -match '^\} else \{$' -or
       $trim -eq '}' -or
       $trim -match '^"\$pkg\$importsTrim' -or
       $trim -match '^"\$pkgimport mobile\.project\.escrowx\.ui\.theme\.EscrowXTheme'){
      continue
    }
    $cleanLines += $line
  }

  $text2=($cleanLines -join "`r`n")
  $text2=$text2 -replace '(?m)^\s*([A-Z][A-Za-z0-9_]*)\s*:\s*ComponentActivity\(\)\s*\{','class $1 : ComponentActivity() {'

  $rel=$f.FullName.Substring($root.Length+1)
  $pkgPath=Split-Path $rel -Parent
  $pkg='package mobile.project.escrowx'
  if($pkgPath){ $pkg='package mobile.project.escrowx.' + ($pkgPath -replace '\\','.') }

  $fileSuppress=''
  if($text2 -match '^(\s*@file:[^\r\n]+\r?\n\r?\n)'){
    $fileSuppress=$matches[1]
    $text2=$text2.Substring($matches[1].Length)
  }

  $text2=$text2 -replace '(?ms)^\s*package\s+[^\r\n]+\r?\n',''
  $text2=$text2 -replace '(?ms)^(\s*import\s+[^\r\n]+\r?\n)+',''

  $header=$fileSuppress + $pkg + "`r`n`r`n" + ($commonImports -join "`r`n") + "`r`n`r`n"
  Set-Content -Path $f.FullName -Value ($header + $text2.TrimStart()) -NoNewline
}

Write-Output ("repaired {0} files" -f $files.Count)
