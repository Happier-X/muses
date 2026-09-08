#!/bin/bash
# Salt → miuix 令牌批量映射（ longest-first，逐文件应用后需补 import 并编译验证）
sed -i \
 -e 's/salt\.surface1Shade/scheme.surfaceContainer/g' \
 -e 's/salt\.surface1Tint/scheme.surfaceContainerHigh/g' \
 -e 's/salt\.surfaceVariant/scheme.surfaceVariant/g' \
 -e 's/salt\.surface1\b/scheme.surface/g' \
 -e 's/salt\.surface2\b/scheme.surfaceVariant/g' \
 -e 's/salt\.surface3\b/scheme.surface/g' \
 -e 's/salt\.surface\b/scheme.background/g' \
 -e 's/salt\.text3\b/scheme.onBackground.copy(alpha = 0.3f)/g' \
 -e 's/salt\.text2\b/scheme.onBackgroundVariant/g' \
 -e 's/salt\.text\b/scheme.onBackground/g' \
 -e 's/salt\.primaryTint\b/scheme.primary/g' \
 -e 's/salt\.primaryShade\b/scheme.primaryVariant/g' \
 -e 's/salt\.primary\b/scheme.primary/g' \
 -e 's/salt\.onPrimary\b/scheme.onPrimary/g' \
 -e 's/salt\.dangerShade\b/scheme.error/g' \
 -e 's/salt\.danger\b/scheme.error/g' \
 -e 's/salt\.success\b/Color(0xFF34C759)/g' \
 -e 's/salt\.disabledText\b/scheme.disabledOnSurface/g' \
 -e 's/salt\.disabledBg\b/scheme.disabledPrimary/g' \
 -e 's/salt\.disabledBorder\b/scheme.dividerLine/g' \
 -e 's/salt\.hairline\b/scheme.dividerLine/g' \
 -e 's/salt\.navbarGlassBg\b/scheme.background.copy(alpha = 0.65f)/g' \
 -e 's/salt\.glassBg\b/scheme.surface.copy(alpha = 0.75f)/g' \
 -e 's/val salt = LocalSaltColors\.current/val scheme = MiuixTheme.colorScheme/g' \
 -e 's/salt === SaltDarkColors/isSystemInDarkTheme()/g' \
 -e 's/SaltSpacing\.navbarTopPaddingMin/16.dp/g' \
 -e 's/SaltSpacing\.listRowHeight/56.dp/g' \
 -e 's/SaltSpacing\.miniPlayerHeight/72.dp/g' \
 -e 's/SaltSpacing\.spacingSub/12.dp/g' \
 -e 's/SaltSpacing\.listIcon/24.dp/g' \
 -e 's/SaltSpacing\.spacing\b/16.dp/g' \
 -e 's/SaltRadius\.dialog/20.dp/g' \
 -e 's/SaltRadius\.card/12.dp/g' \
 -e 's/SaltRadius\.sm\b/8.dp/g' \
 -e 's/SaltRadius\.md\b/16.dp/g' \
 -e 's/SaltRadius\.lg\b/24.dp/g' \
 "$1"
echo "mapped $1"
