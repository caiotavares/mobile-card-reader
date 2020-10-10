
import 'dart:async';

import 'package:flutter/services.dart';

class Emv {
  static const MethodChannel _channel =
      const MethodChannel('emv');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
