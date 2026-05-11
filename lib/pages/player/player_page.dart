import 'package:flutter/material.dart';
import 'package:signals_flutter/signals_flutter.dart' hide computed;

import '../../app/services/player_service.dart';
import '../../app/state/settings_state.dart';
import '../../app/state/song_state.dart';
import '../../components/common/artwork_widget.dart';
import 'lyrics/lyric_view.dart';
import 'widgets/player_background.dart';
import 'widgets/player_bottom_panel.dart';
import 'widgets/player_header.dart';

class PlayerPage extends StatefulWidget {
  const PlayerPage({super.key});

  @override
  State<PlayerPage> createState() => _PlayerPageState();
}

class _PlayerPageState extends State<PlayerPage>
    with SingleTickerProviderStateMixin {
  final PlayerService _player = PlayerService.instance;
  final PageController _pageController = PageController();
  late final AnimationController _dismissController;
  double _dismissDragOffset = 0;
  double? _dismissDragStartY;

  @override
  void initState() {
    super.initState();
    _dismissController =
        AnimationController.unbounded(
          vsync: this,
          duration: const Duration(milliseconds: 220),
        )..addListener(() {
          if (!mounted) return;
          setState(() {
            _dismissDragOffset = _dismissController.value;
          });
        });
  }

  void _handleDismissDragStart(DragStartDetails details) {
    _dismissController.stop();
    _dismissDragStartY = details.globalPosition.dy;
  }

  void _handleDismissDragUpdate(DragUpdateDetails details) {
    final startY = _dismissDragStartY;
    if (startY == null) return;
    final offset = details.globalPosition.dy - startY;
    if (offset <= 0) return;
    final maxOffset = MediaQuery.sizeOf(context).height;
    setState(() {
      _dismissDragOffset = offset.clamp(0, maxOffset);
    });
    _dismissController.value = _dismissDragOffset;
  }

  void _handleDismissDragEnd(DragEndDetails details) {
    final velocity = details.primaryVelocity ?? 0;
    final shouldDismiss = _dismissDragOffset > 72 || velocity > 700;
    final startOffset = _dismissDragOffset;
    _dismissDragStartY = null;
    if (shouldDismiss) {
      final screenHeight = MediaQuery.sizeOf(context).height;
      final remaining = (screenHeight - startOffset).clamp(120.0, screenHeight);
      _dismissController.duration = Duration(
        milliseconds: velocity > 0
            ? (remaining / velocity * 1000).clamp(120, 240).round()
            : 180,
      );
      _dismissController.value = startOffset;
      _dismissController
          .animateTo(screenHeight, curve: Curves.easeOutCubic)
          .whenComplete(() {
            if (!mounted) return;
            _closePlayer();
          });
      return;
    }
    _dismissController.duration = const Duration(milliseconds: 220);
    _dismissController.value = startOffset;
    _dismissController.animateBack(0, curve: Curves.easeOutCubic);
  }

  void _handleDismissDragCancel() {
    if (_dismissDragOffset == 0) return;
    _dismissDragStartY = null;
    _dismissController.duration = const Duration(milliseconds: 220);
    _dismissController.value = _dismissDragOffset;
    _dismissController.animateBack(0, curve: Curves.easeOutCubic);
  }

  void _closePlayer() {
    final navigator = Navigator.of(context);
    if (navigator.canPop()) {
      navigator.pop();
      return;
    }
    final rootNavigator = Navigator.of(context, rootNavigator: true);
    if (rootNavigator.canPop()) {
      rootNavigator.pop();
    }
  }

  @override
  void dispose() {
    _dismissController.dispose();
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final dismissProgress = (_dismissDragOffset / 120).clamp(0.0, 1.0);
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        _closePlayer();
      },
      child: Scaffold(
        backgroundColor: Colors.transparent,
        resizeToAvoidBottomInset: false,
        body: Transform.translate(
          offset: Offset(0, _dismissDragOffset),
          child: Opacity(
            opacity: 1 - dismissProgress * 0.08,
            child: ClipRRect(
              borderRadius: BorderRadius.vertical(
                top: Radius.circular(dismissProgress * 24),
              ),
              child: Stack(
                children: [
                  RepaintBoundary(
                    child: PlayerBackground(
                      songSignal: _player.currentSongSignal,
                    ),
                  ),
                  PlayerTheme(
                    child: SafeArea(
                      child: Column(
                        children: [
                          GestureDetector(
                            behavior: HitTestBehavior.translucent,
                            onVerticalDragStart: _handleDismissDragStart,
                            onVerticalDragUpdate: _handleDismissDragUpdate,
                            onVerticalDragEnd: _handleDismissDragEnd,
                            onVerticalDragCancel: _handleDismissDragCancel,
                            child: PlayerHeader(
                              songSignal: _player.currentSongSignal,
                            ),
                          ),
                          Expanded(
                            child: PageView(
                              controller: _pageController,
                              children: [
                                _PlayerView(
                                  player: _player,
                                  onTapLyrics: () =>
                                      _pageController.animateToPage(
                                        1,
                                        duration: const Duration(
                                          milliseconds: 280,
                                        ),
                                        curve: Curves.easeOut,
                                      ),
                                ),
                                const PlayerLyricsView(),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _PlayerView extends StatelessWidget {
  final PlayerService player;
  final VoidCallback onTapLyrics;

  const _PlayerView({required this.player, required this.onTapLyrics});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: AppLayoutSettings.tabletMode,
      builder: (context, tabletMode, _) {
        final mq = MediaQuery.of(context);
        final isTabletLandscape =
            tabletMode &&
            mq.orientation == Orientation.landscape &&
            mq.size.width >= 900;
        if (!isTabletLandscape) {
          return Column(
            children: [
              const Spacer(flex: 1),
              _PlayerArtwork(songSignal: player.currentSongSignal),
              const Spacer(flex: 1),
              PlayerBottomPanel(player: player, onTapLyrics: onTapLyrics),
            ],
          );
        }
        return Padding(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 12),
          child: Row(
            children: [
              Expanded(
                flex: 5,
                child: Column(
                  children: [
                    const Spacer(),
                    Expanded(
                      flex: 8,
                      child: Center(
                        child: _PlayerArtwork(
                          songSignal: player.currentSongSignal,
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    PlayerHeader(songSignal: player.currentSongSignal),
                    const Spacer(),
                  ],
                ),
              ),
              const SizedBox(width: 24),
              Expanded(
                flex: 6,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(24),
                  child: Container(
                    color: Theme.of(
                      context,
                    ).colorScheme.surface.withValues(alpha: 0.16),
                    child: const PlayerLyricsView(),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _PlayerArtwork extends StatelessWidget {
  final Signal<SongEntity?> songSignal;

  const _PlayerArtwork({required this.songSignal});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: AppLayoutSettings.tabletMode,
      builder: (context, tabletMode, _) {
        final isTabletLayout =
            tabletMode && MediaQuery.sizeOf(context).width >= 720;
        return Watch.builder(
          builder: (context) {
            final song = songSignal.value;
            final border = BorderRadius.circular(12);
            final maxSize = isTabletLayout ? 320.0 : double.infinity;
            if (song == null) {
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 32),
                child: LayoutBuilder(
                  builder: (context, constraints) {
                    final size = constraints.maxWidth;
                    final boxSize = size < maxSize ? size : maxSize;
                    return Center(
                      child: SizedBox(
                        width: boxSize,
                        height: boxSize,
                        child: _ArtworkShadowContainer(
                          border: border,
                          child: _ArtworkPlaceholder(border: border, label: ''),
                        ),
                      ),
                    );
                  },
                ),
              );
            }
            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: 32),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final size = constraints.maxWidth;
                  final boxSize = size < maxSize ? size : maxSize;
                  return Center(
                    child: SizedBox(
                      width: boxSize,
                      height: boxSize,
                      child: _ArtworkShadowContainer(
                        border: border,
                        child: ArtworkWidget(
                          song: song,
                          size: boxSize,
                          borderRadius: 12,
                          preferOriginal: true,
                          keepPreviousUntilLoaded: true,
                          placeholder: _ArtworkPlaceholder(
                            border: border,
                            label: song.title,
                          ),
                        ),
                      ),
                    ),
                  );
                },
              ),
            );
          },
        );
      },
    );
  }
}

class _ArtworkShadowContainer extends StatelessWidget {
  final BorderRadius border;
  final Widget child;

  const _ArtworkShadowContainer({required this.border, required this.child});

  @override
  Widget build(BuildContext context) {
    return ClipRRect(borderRadius: border, child: child);
  }
}

class _ArtworkPlaceholder extends StatelessWidget {
  final BorderRadius border;
  final String label;

  const _ArtworkPlaceholder({required this.border, required this.label});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = label.trim().isEmpty ? '?' : label.trim().substring(0, 1);
    return Container(
      decoration: BoxDecoration(
        borderRadius: border,
        color: scheme.primary.withValues(alpha: 0.12),
      ),
      child: Center(
        child: Text(
          text.toUpperCase(),
          style: TextStyle(
            fontSize: 48,
            fontWeight: FontWeight.w700,
            color: scheme.primary,
          ),
        ),
      ),
    );
  }
}
