/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.util;

import org.apache.oro.text.perl.Perl5Util;

public class XMLDateFilter implements Filter {
    private Perl5Util util = new Perl5Util();
    private final static String PATTERN  =
            "/\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}/";

    public XMLDateFilter() {
    }

    public String filter(final String in) {
      if (in != null && util.match(PATTERN, in)) {
        return util.substitute("s" + PATTERN + "yyyy-MM-ddTHH:mm:ss.SSS/", in);
      } else {
        return in;
      }
    }

}
