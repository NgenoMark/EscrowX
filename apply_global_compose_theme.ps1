$base='c:\Projects\EscrowX\Application\EscrowX\app\src\main\java\mobile\project\escrowx'
$files=@(
'dash\BuyerDashboard.kt',
'dash\CreateEscrowActivity.kt',
'dash\DisputeCenterActivity.kt',
'dash\IncomingRequestsActivity.kt',
'dash\PaymentActivity.kt',
'dash\RaiseDisputeActivity.kt',
'dash\SettingsActivity.kt',
'dash\TransactionActivity.kt',
'dash\TransactionTrackDetails.kt',
'dash\TransactionsDetailsActivity.kt',
'seller\CreatePaymentLinkActivity.kt',
'seller\IncomingEscrowActivity.kt',
'seller\LinkGenerateActivity.kt',
'seller\RejectRequestActivity.kt',
'seller\RequestAcceptedActivity.kt',
'seller\RequestDeclineActivity.kt',
'seller\SellerActiveEscrowsActivity.kt',
'seller\SellerDashboardActivity.kt',
'seller\SellerTransactionDetailActivity.kt',
'auth\ChangePasswordVerificationActivity.kt',
'auth\CreateNewPasswordActivity.kt'
)

foreach($rel in $files){
  $p=Join-Path $base $rel
  if(-not (Test-Path $p)){ continue }
  $t=Get-Content -Raw $p
  $t=$t -replace 'setContent\s*\{\s*MaterialTheme\s*\{','setContent {
            EscrowXTheme(darkTheme = ThemePreferenceManager.isDarkModeEnabled(this), dynamicColor = false) {'

  if($t -notmatch 'import mobile\.project\.escrowx\.ui\.theme\.EscrowXTheme'){
    $t=$t -replace '(?m)^(package\s+[^\r\n]+\r?\n)','$1`r`nimport mobile.project.escrowx.ui.theme.EscrowXTheme`r`nimport mobile.project.escrowx.ui.theme.ThemePreferenceManager`r`n'
  }

  Set-Content -Path $p -Value $t -NoNewline
}

Write-Output "updated"
