package com.prslc.zhiflow.core.exception

import okhttp3.Response
import java.io.IOException

class HttpStatusException(
    val response: Response,
    /** The code the body carried, when it carried one. Read before the body is closed. */
    val apiErrorCode: Int? = null,
    /** The message the body carried, when it carried one. Read before the body is closed. */
    val apiErrorMessage: String? = null,
) : IOException()
