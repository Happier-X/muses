import 'package:flutter/material.dart';

import '../../components/layout/base/app_background.dart';

PageRoute<T> buildAppPageRoute<T>(
  WidgetBuilder builder, {
  RouteSettings? settings,
}) {
  return PageRouteBuilder<T>(
    settings: settings,
    transitionDuration: const Duration(milliseconds: 300),
    reverseTransitionDuration: const Duration(milliseconds: 240),
    pageBuilder: (context, animation, secondaryAnimation) =>
        AppBackground(child: builder(context)),
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      final primary = CurvedAnimation(
        parent: animation,
        curve: Curves.easeOutCubic,
        reverseCurve: Curves.easeInCubic,
      );
      final secondary = CurvedAnimation(
        parent: secondaryAnimation,
        curve: Curves.easeOutCubic,
        reverseCurve: Curves.easeInCubic,
      );
      final incomingOffset = Tween<Offset>(
        begin: const Offset(0.08, 0),
        end: Offset.zero,
      ).animate(primary);
      final outgoingOffset = Tween<Offset>(
        begin: Offset.zero,
        end: const Offset(-0.035, 0),
      ).animate(secondary);
      final incomingOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
        CurvedAnimation(
          parent: animation,
          curve: const Interval(0.0, 0.8, curve: Curves.easeOutCubic),
          reverseCurve: Curves.easeInCubic,
        ),
      );
      final outgoingOpacity = Tween<double>(begin: 1.0, end: 0.92).animate(
        CurvedAnimation(
          parent: secondaryAnimation,
          curve: const Interval(0.0, 1.0, curve: Curves.easeOutCubic),
          reverseCurve: Curves.easeInCubic,
        ),
      );

      return SlideTransition(
        position: outgoingOffset,
        child: FadeTransition(
          opacity: outgoingOpacity,
          child: SlideTransition(
            position: incomingOffset,
            child: FadeTransition(opacity: incomingOpacity, child: child),
          ),
        ),
      );
    },
  );
}
