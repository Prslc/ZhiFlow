package com.prslc.zhiflow.core.exception

import okhttp3.Response
import java.io.IOException

class HttpStatusException(val response: Response) : IOException()
