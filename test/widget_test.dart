// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter_test/flutter_test.dart';

import 'package:muses/app/app.dart';
import 'package:muses/pages/home/home_page.dart';

void main() {
  testWidgets('Home page renders', (WidgetTester tester) async {
    await tester.pumpWidget(const MusesApp());
    await tester.pumpAndSettle();
    expect(find.byType(HomePage), findsOneWidget);
  });
}
