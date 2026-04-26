import 'package:flutter/material.dart';

PageRoute<T> buildAppPageRoute<T>(
  WidgetBuilder builder, {
  RouteSettings? settings,
}) {
  return MaterialPageRoute<T>(settings: settings, builder: builder);
}
