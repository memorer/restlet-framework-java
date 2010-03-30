/**
 * Copyright 2005-2010 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.ext.sip;

import java.io.IOException;

import org.restlet.data.Parameter;
import org.restlet.engine.http.header.HeaderReader;

/**
 * Retry-after header reader.
 * 
 * @author Thierry Boileau
 */
public class AvailabilityReader extends HeaderReader<Availability> {

    public static void main(String[] args) throws Exception {
        String str = "18000;duration=3600;tag=hyh8";
        AvailabilityReader r = new AvailabilityReader(str);
        Availability a = r.readValue();
        System.out.println(a.getDelay() + ".");
        System.out.println(a.getDuration() + ".");
        System.out.println(a.getComment() + ".");
        System.out.println(a.getParameters());

        str = "120 (I'm in a meeting)";
        r = new AvailabilityReader(str);
        a = r.readValue();
        System.out.println(a.getDelay() + ".");
        System.out.println(a.getDuration() + ".");
        System.out.println(a.getComment() + ".");
        System.out.println(a.getParameters());
    }
    
    /**
     * Constructor.
     * 
     * @param header
     *            The header to read.
     */
    public AvailabilityReader(String header) {
        super(header);
    }

    @Override
    public Availability readValue() throws IOException {
        Availability result = null;

        skipSpaces();
        if (peek() != -1) {
            String delay = readToken();
            result = new Availability(Integer.parseInt(delay));
            skipSpaces();
            if (peek() == '(') {
                result.setComment(readComment());
            }
            skipSpaces();
        }

        // Read availability parameters.
        if (skipParameterSeparator()) {
            Parameter param = readParameter();

            while (param != null) {
                if ("duration".equals(param.getName())) {
                    result.setDuration(Integer.parseInt(param.getValue()));
                } else {
                    result.getParameters().add(param);
                }

                if (skipParameterSeparator()) {
                    param = readParameter();
                } else {
                    param = null;
                }
            }
        }

        return result;
    }

}