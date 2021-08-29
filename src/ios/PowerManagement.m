/*
 * Copyright 2013 Wolfgang Koller
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Cordova (iOS) plugin for accessing the power-management functions of the device
 */
#import "PowerManagement.h"

/**
 * Actual implementation of the interface
 */
@implementation PowerManagement
- (void) acquire:(CDVInvokedUrlCommand*)command
{
    // Acquire a reference to the local UIApplication singleton
    UIApplication* app = [UIApplication sharedApplication];

    CDVPluginResult *pluginResult = nil;
    if( ![app isIdleTimerDisabled] ) {
        [app setIdleTimerDisabled:true];
    }
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void) release:(CDVInvokedUrlCommand*)command
{
    // Acquire a reference to the local UIApplication singleton
    UIApplication* app = [UIApplication sharedApplication];


    CDVPluginResult *pluginResult = nil;
    if( [app isIdleTimerDisabled] ) {
        [app setIdleTimerDisabled:false];
    }
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
