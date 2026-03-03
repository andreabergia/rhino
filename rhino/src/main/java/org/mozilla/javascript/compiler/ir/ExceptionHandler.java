/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.compiler.ir;

import java.util.List;

/** Maps a set of protected blocks to a handler block for exception dispatch. */
public record ExceptionHandler(
        List<BlockId> protectedBlocks, BlockId handlerBlock, boolean isFinally) {}
