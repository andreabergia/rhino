/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

/** Enumeration kind for for-in / for-of iteration. */
public enum EnumKind {
    KEYS,
    VALUES,
    ARRAY,
    VALUES_IN_ORDER
}
