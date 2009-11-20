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
package org.restlet.ext.jaxrs.internal.wrappers.params;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;

import org.restlet.data.Request;
import org.restlet.ext.jaxrs.internal.exceptions.ConvertRepresentationException;
import org.restlet.ext.jaxrs.internal.wrappers.params.ParameterList.ParamGetter;
import org.restlet.resource.Representation;

/**
 * An {@link EntityGetter} for Restlet {@link Representation}s.
 * 
 * @author Stephan Koops
 */
public abstract class ReprEntityGetter implements ParamGetter {

    /**
     * EntityGetter, if there is a constructor with two arguments: Class and
     * Representation
     */
    static class ClassReprEntityGetter extends ReprEntityGetter {
        private final Class<?> clazz;

        ClassReprEntityGetter(Class<?> genClass, Constructor<?> constructor) {
            super(constructor);
            this.clazz = genClass;
        }

        @Override
        Representation createInstance(Representation entity)
                throws IllegalArgumentException, InstantiationException,
                IllegalAccessException, InvocationTargetException {
            return this.constr.newInstance(this.clazz, entity);
        }

    }

    /**
     * EntityGetter, if Representation could directly be injected
     */
    static class DirectReprEntityGetter implements ParamGetter {

        /**
         * @see org.restlet.ext.jaxrs.internal.wrappers.params.ParameterList.ParamGetter#getValue()
         */
        public Object getValue() throws InvocationTargetException,
                ConvertRepresentationException, WebApplicationException {
            return Request.getCurrent().getEntity();
        }

    }

    /**
     * EntityGetter, if there is a constructor with two arguments:
     * Representation and Class
     */
    static class ReprClassEntityGetter extends ReprEntityGetter {

        private final Class<?> clazz;

        ReprClassEntityGetter(Constructor<?> constructor, Class<?> genClass) {
            super(constructor);
            this.clazz = genClass;
        }

        @Override
        Representation createInstance(Representation entity)
                throws IllegalArgumentException, InstantiationException,
                IllegalAccessException, InvocationTargetException {
            return this.constr.newInstance(entity, this.clazz);
        }

    }

    /**
     * EntityGetter, if there is a constructor for only the entity.
     */
    static class ReprOnlyEntityGetter extends ReprEntityGetter {

        /**
         * @param constructor
         */
        ReprOnlyEntityGetter(Constructor<?> constructor) {
            super(constructor);
        }

        @Override
        Representation createInstance(Representation entity)
                throws IllegalArgumentException, InstantiationException,
                IllegalAccessException, InvocationTargetException {
            return this.constr.newInstance(entity);
        }

    }

    /**
     * Creates an EntityGetter for the given values, or null, if it is not
     * available.
     * 
     * @param representationType
     * @param convToGen
     * @param logger
     * @return
     */
    public static ParamGetter create(Class<?> representationType,
            Type convToGen, Logger logger) {
        if (representationType.equals(Representation.class)) {
            return new DirectReprEntityGetter();
        }
        try {
            return new ReprOnlyEntityGetter(representationType
                    .getConstructor(Representation.class));
        } catch (final SecurityException e) {
            logger.warning("The constructor " + representationType
                    + "(Representation) is not accessable.");
        } catch (final NoSuchMethodException e) {
            // try next
        }
        if (!(convToGen instanceof ParameterizedType)) {
            return null;
        }
        final ParameterizedType pType = (ParameterizedType) convToGen;
        final Type[] typeArgs = pType.getActualTypeArguments();
        if (typeArgs.length != 1) {
            return null;
        }
        final Type typeArg = typeArgs[0];
        if (!(typeArg instanceof Class)) {
            return null;
        }
        final Class<?> genClass = (Class<?>) typeArg;
        try {
            return new ReprClassEntityGetter(representationType.getConstructor(
                    Representation.class, Class.class), genClass);
        } catch (final SecurityException e) {
            logger.warning("The constructor " + representationType
                    + "(Representation) is not accessable.");
        } catch (final NoSuchMethodException e) {
            // try next
        }
        try {
            return new ClassReprEntityGetter(genClass, representationType
                    .getConstructor(Class.class, Representation.class));
        } catch (final SecurityException e) {
            logger.warning("The constructor " + representationType
                    + "(Representation) is not accessable.");
            return null;
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    final Constructor<? extends Representation> constr;

    @SuppressWarnings("unchecked")
    ReprEntityGetter(Constructor<?> constr) {
        this.constr = (Constructor) constr;
    }

    abstract Representation createInstance(Representation entity)
            throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException;

    /**
     * @return the class of the {@link Representation}.
     */
    private Class<? extends Representation> getReprClass() {
        return this.constr.getDeclaringClass();
    }

    /**
     * @see org.restlet.ext.jaxrs.internal.wrappers.params.ParameterList.ParamGetter#getValue()
     */
    public Object getValue() throws InvocationTargetException,
            ConvertRepresentationException, WebApplicationException {
        try {
            final Request request = Request.getCurrent();
            if (!request.isEntityAvailable()) {
                return null;
            }
            final Representation entity = request.getEntity();
            if((entity == null)/* || (entity is not empty)*/) {
                return null;
            }
            return createInstance(entity);
        } catch (final IllegalArgumentException e) {
            throw ConvertRepresentationException.object(getReprClass(),
                    "the message body", e);
        } catch (final InstantiationException e) {
            throw ConvertRepresentationException.object(getReprClass(),
                    "the message body", e);
        } catch (final IllegalAccessException e) {
            throw ConvertRepresentationException.object(getReprClass(),
                    "the message body", e);
        }
    }
}