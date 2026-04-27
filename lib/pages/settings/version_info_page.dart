import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../app/services/app_update_service.dart';
import '../../app/services/debug_log_service.dart';
import '../../components/index.dart';

class VersionInfoPage extends StatefulWidget {
  const VersionInfoPage({super.key});

  @override
  State<VersionInfoPage> createState() => _VersionInfoPageState();
}

class _VersionInfoPageState extends State<VersionInfoPage> {
  static const String _appName = 'NagoMusic';
  static const String _version = '1.2.7+1';
  static const String _iconAsset = '开发文档/NagoAPP图标.png';

  final DebugLogService _debugLogs = DebugLogService.instance;

  bool _checking = false;
  AppUpdateInfo? _updateInfo;

  @override
  void initState() {
    super.initState();
    _debugLogs.ensureLoaded();
  }

  Future<void> _checkUpdate() async {
    if (_checking) return;
    setState(() => _checking = true);
    try {
      final info = await AppUpdateService.instance.checkLatest(_version);
      if (!mounted) return;
      setState(() => _updateInfo = info);
      await _showUpdateResultDialog(info);
    } catch (_) {
      if (!mounted) return;
      await _showUpdateFailedDialog();
    } finally {
      if (mounted) {
        setState(() => _checking = false);
      }
    }
  }

  Future<void> _clearLogs() async {
    await _debugLogs.clear();
    if (!mounted) return;
    AppToast.show(context, '日志已清空');
  }

  Future<void> _exportLogs() async {
    await _debugLogs.ensureLoaded();
    final now = DateTime.now();
    final filename =
        'nagomusic-debug-${now.year}${_two(now.month)}${_two(now.day)}-${_two(now.hour)}${_two(now.minute)}${_two(now.second)}.txt';
    final dir = await getApplicationDocumentsDirectory();
    final file = File(p.join(dir.path, filename));
    await file.writeAsString(_debugLogs.exportText(), flush: true);
    if (!mounted) return;
    await _showLogExportDialog(file.path);
  }

  Future<void> _showUpdateResultDialog(AppUpdateInfo info) async {
    await showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(info.hasUpdate ? '发现新版本' : '当前已是最新版本'),
          content: Text(
            info.hasUpdate
                ? '当前版本：$_version\n最新版本：${info.latestVersion}${info.releaseName == null ? '' : '\n版本名称：${info.releaseName}'}'
                : '当前版本 $_version 已是最新版本。',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('关闭'),
            ),
            if (info.hasUpdate)
              FilledButton(
                onPressed: () {
                  Navigator.pop(context);
                  _openUrl(info.releaseUrl ?? AppUpdateService.releasePageUrl);
                },
                child: const Text('前往下载'),
              ),
          ],
        );
      },
    );
  }

  Future<void> _showUpdateFailedDialog() async {
    await showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('检查更新失败'),
          content: const Text('无法连接更新服务。你可以手动打开发布页面检查新版本。'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('关闭'),
            ),
            TextButton(
              onPressed: () async {
                await Clipboard.setData(
                  const ClipboardData(text: AppUpdateService.releasePageUrl),
                );
                if (!context.mounted) return;
                Navigator.pop(context);
                AppToast.show(context, '更新地址已复制');
              },
              child: const Text('复制地址'),
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context);
                _openUrl(AppUpdateService.releasePageUrl);
              },
              child: const Text('手动打开'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _showLogExportDialog(String path) async {
    await showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('日志已导出'),
          content: SelectableText(path),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('关闭'),
            ),
            TextButton(
              onPressed: () async {
                await Clipboard.setData(ClipboardData(text: path));
                if (!context.mounted) return;
                Navigator.pop(context);
                AppToast.show(context, '文件路径已复制');
              },
              child: const Text('复制路径'),
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context);
                _openFile(path);
              },
              child: const Text('打开文件'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _openUrl(String url) async {
    final uri = Uri.parse(url);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else if (mounted) {
      await Clipboard.setData(ClipboardData(text: url));
      if (!mounted) return;
      AppToast.show(context, '无法打开浏览器，地址已复制');
    }
  }

  Future<void> _openFile(String path) async {
    final uri = Uri.file(path);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else if (mounted) {
      await Clipboard.setData(ClipboardData(text: path));
      if (!mounted) return;
      AppToast.show(context, '无法打开文件，路径已复制');
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomPadding = AppPageScaffold.scrollableBottomPadding(context);
    return AppPageScaffold(
      extendBodyBehindAppBar: true,
      appBar: const AppTopBar(
        title: '版本信息',
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: ListView(
        padding: EdgeInsets.fromLTRB(16, 12, 16, bottomPadding),
        children: [
          AppSettingSection(
            title: '应用信息',
            children: [
              AppSettingTile(
                title: '应用名称',
                subtitle: _appName,
                leading: ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.asset(
                    _iconAsset,
                    width: 32,
                    height: 32,
                    fit: BoxFit.cover,
                  ),
                ),
              ),
              const AppSettingTile(
                title: '当前版本',
                subtitle: _version,
                leading: Icon(Icons.info_outline_rounded),
              ),
              AppSettingTile(
                title: '检查更新',
                subtitle: _updateSubtitle(),
                leading: const Icon(Icons.system_update_alt_rounded),
                trailing: _checking
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.chevron_right_rounded),
                onTap: _checking ? null : _checkUpdate,
              ),
            ],
          ),
          const SizedBox(height: 16),
          AppSettingSection(
            title: '调试',
            children: [
              ValueListenableBuilder<bool>(
                valueListenable: _debugLogs.enabled,
                builder: (context, enabled, _) {
                  return AppSettingSwitchTile(
                    title: '调试模式',
                    subtitle: '开启后记录最近的调试日志，便于排查卡顿和异常',
                    value: enabled,
                    onChanged: _debugLogs.setEnabled,
                  );
                },
              ),
              AppSettingTile(
                title: '清空日志',
                subtitle: '删除已记录的本地调试日志',
                leading: const Icon(Icons.delete_outline_rounded),
                onTap: _clearLogs,
              ),
              AppSettingTile(
                title: '导出日志',
                subtitle: '生成日志文件，便于发送给开发者',
                leading: const Icon(Icons.download_rounded),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: _exportLogs,
              ),
            ],
          ),
          const SizedBox(height: 16),
          ValueListenableBuilder<List<String>>(
            valueListenable: _debugLogs.entries,
            builder: (context, logs, _) {
              return AppSettingSection(
                title: '最近日志',
                padding: const EdgeInsets.all(16),
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      logs.isEmpty ? '暂无日志' : logs.join('\n'),
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        fontFamily: 'monospace',
                        height: 1.35,
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  String _updateSubtitle() {
    final info = _updateInfo;
    if (info == null) return '检查是否有新版本';
    if (info.hasUpdate) {
      return '最新版本 ${info.latestVersion}${info.releaseName == null ? '' : ' · ${info.releaseName}'}';
    }
    return '当前已是最新版本';
  }

  String _two(int value) => value.toString().padLeft(2, '0');
}
