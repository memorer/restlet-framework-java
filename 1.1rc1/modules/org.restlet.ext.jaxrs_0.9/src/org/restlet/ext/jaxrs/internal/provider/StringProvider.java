/**
 * Copyright 2005-2008 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of the following open
 * source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.sun.com/cddl/cddl.html
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royaltee free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */
package org.restlet.ext.jaxrs.internal.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.restlet.data.CharacterSet;
import org.restlet.data.Response;
import org.restlet.ext.jaxrs.internal.util.Util;

/**
 * Provider for {@link String}s. Could also write other {@link CharSequence}s.
 * 
 * @author Stephan Koops
 */
@Provider
@Produces("*/*")
@Consumes("*/*")
public class StringProvider extends AbstractProvider<CharSequence> {

    /**
     * Returns an {@link InputStream}, that returns the right encoded data
     * according to the given {@link CharacterSet}.
     * 
     * @param charSequ
     * @param charsetName
     *            see {@link String#getBytes(String)}
     * @return
     */
    private ByteArrayInputStream getInputStream(CharSequence charSequ,
            String charsetName) {
        byte[] bytes;
        final String string = charSequ.toString();
        try {
            bytes = string.getBytes(charsetName);
        } catch (final UnsupportedEncodingException e) {
            try {
                bytes = string.getBytes(Util.JAX_RS_DEFAULT_CHARACTER_SET
                        .toString());
            } catch (final UnsupportedEncodingException e1) {
                bytes = string.getBytes();
            }
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * @see javax.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object)
     */
    @Override
    public long getSize(CharSequence t) {
        return t.length();
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations) {
        return type.isAssignableFrom(String.class);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations) {
        return CharSequence.class.isAssignableFrom(type);
    }

    @Override
    public String readFrom(Class<CharSequence> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException {
        return Util.copyToStringBuilder(entityStream).toString();
    }

    /**
     * @see javax.ws.rs.ext.MessageBodyWriter#writeTo(Object, Class, Type,
     *      Annotation[], MediaType, MultivaluedMap, OutputStream)
     */
    @Override
    public void writeTo(CharSequence charSequence, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        final CharacterSet cs = Response.getCurrent().getEntity()
                .getCharacterSet();
        final InputStream inputStream = getInputStream(charSequence, cs
                .toString());
        Util.copyStream(inputStream, entityStream);
    }
}