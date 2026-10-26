"""Mutation matrix for the Kaspresso suite: for each defect, run the test class that's
meant to catch it with that defect injected (via the cbDefects instrumentation
argument), and report which tests went red. Assumes the app and test APKs are installed.

Usage:
    ./gradlew :app:installStandardDebug :app:installStandardDebugAndroidTest
    python3 tools/mutation_matrix.py results.tsv

A row whose last column is "-" is a surviving mutant: that defect got past its tests.
"""
import re
import subprocess
import sys

PKG = "com.vadimtoptunov.chaosbank_android.kaspresso.tests"
RUNNER = "com.vadimtoptunov.chaosbank_android.test/androidx.test.runner.AndroidJUnitRunner"

MATRIX = {
    "TransferTests": ["successToastMissing", "zeroAmountAccepted"],
    "ExchangeTests": ["roundingDrift", "exchangeDoubleSubmit"],
    "CardTests": ["cardToggleInvert", "cardCvvVisible"],
    "TransactionsTests": ["paginationDup", "filterLeaksCategory"],
    "OrderTests": ["wrongA11yLabel", "limitValidation", "orderStuckPending"],
    "HomeValueTests": ["homeTotalOmitsAccount", "balanceFloorRounded", "todayChangeSignFlipped"],
    "TransferValueTests": ["balanceAfterAdds", "transferRoundsUp", "transferDebitsWrongAccount"],
    "ExchangeValueTests": ["exchangeCreditsWrongAccount"],
    "LoansValueTests": ["loanAprUnderstated"],
    "MarketsValueTests": ["changePctSignFlipped", "detailPriceOffset"],
    "OrderValueTests": ["estTotalIgnoresQty", "limitExecutesAtMarket"],
    "PortfolioValueTests": ["pnlSign", "totalValueOmitsHolding", "holdingValueUsesCost", "pnlPercentVsValue"],
    "LocalizationTests": ["localeParse", "balanceWrongCurrencySymbol", "priceMissingDecimals",
                          "searchTrimsNothing", "numberGroupingIgnoresLocale",
                          "currencySymbolPlacementIgnoresLocale"],
    "ConcurrencyTests": ["doubleCharge", "orderDoubleSubmit", "livePriceRace", "syncLostUpdate"],
    "SecurityTests": ["cardNumberFullyVisible", "pinShownPlaintext", "virtualCardShowsRealPan",
                      "otpAcceptsAnyCode", "otpNoLockout", "deepLinkSkipsAuth", "tokenInUserDefaults"],
}


def run(cls, defect):
    out = subprocess.run(
        ["adb", "shell", "am", "instrument", "-w", "-e", "class", f"{PKG}.{cls}",
         "-e", "cbDefects", defect, RUNNER],
        capture_output=True, text=True, timeout=1800).stdout
    failed = re.findall(r"^\d+\) (\w+)\(", out, re.M)
    ok = re.search(r"OK \((\d+) tests?\)", out)
    total = int(ok.group(1)) if ok else int((re.search(r"Tests run: (\d+)", out) or [0, 0])[1])
    return total, failed


with open(sys.argv[1], "w") as f:
    for cls, defects in MATRIX.items():
        for d in defects:
            total, failed = run(cls, d)
            line = f"{cls}\t{d}\t{total}\t{','.join(failed) or '-'}"
            print(line, flush=True)
            f.write(line + "\n")
