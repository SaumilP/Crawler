package crawler;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.reflect.TypeToken;
import crawler.annotations.AfterPageLoad;
import crawler.annotations.Page;
import crawler.annotations.Selector;
import crawler.elements.Instantiator;
import crawler.elements.Link;
import crawler.exceptions.WrongTypeForField;
import crawler.exceptions.TooManyResultsException;
import crawler.http.BrowserClient;
import crawler.http.GetException;
import crawler.http.SimpleHttpClientImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static crawler.elements.Instantiator.instanceForNode;
import static crawler.elements.Instantiator.typeIsKnown;

/**
 * Created by PATEL1 on 12/27/14.
 */
public class Browser {
    private static BrowserClient client;
    private static String currentPageUrl;
    private static String currentPageContents;

    public static <T> T get( final Class<T> pageClass, final String... params ){
        cryIfNoAnnotated(pageClass);
        try{
            final String pageUrl = pageClass.getAnnotation( Page.class ).value();
            return loadPage( pageUrl, pageClass, params);
        } catch ( Exception ex ){
            throw new RuntimeException(ex );
        }
    }

    public static <T> T get( final String pageUrl, final Class<T> pageClass, final String... params){
        cryIfNoAnnotated( pageClass );
        return loadPage( pageUrl, pageClass, params );
    }

    private static <T> void cryIfNoAnnotated(Class<T> pageClass) {
        if ( !pageClass.isAnnotationPresent(Page.class)){
            throw new RuntimeException("To be mapped from a page, the class must be annotated @" + Page.class.getSimpleName());
        }
    }

    private static String loadPage(String pageUrl, final String... params){
        if(client == null)
            setClient(new SimpleHttpClientImpl());
        try{
            return client.get(pageUrl);
        }catch(Exception e){
            throw new GetException(e, pageUrl);
        }
    }

    private static <T> T loadPage(final String pageUrl, final Class<T> pageClass, final String... params){
        String[] formattedParams = new String[params.length];
        for(int i = 0 ; i < params.length; i++){
            try {
                formattedParams[i] = URLEncoder.encode(params[i],"UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        Browser.currentPageUrl = MessageFormat.format(pageUrl, (Object[])formattedParams);

        final Document parse;
        final String page = loadPage(Browser.currentPageUrl);
        currentPageContents = page;
        parse = Jsoup.parse(page);

        T t = loadDomContents(parse, pageClass);

        Method[] declaredMethods = pageClass.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if(declaredMethod.getAnnotation(AfterPageLoad.class) != null){
                try {
                    declaredMethod.invoke(t);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return t;
    }

    private static <T> T loadDomContents(final Element node, final Class<T> classs){
        try {
            return internalLoadDomContents(node, classs);
        }catch (TooManyResultsException e) {
            throw e;
        }catch (WrongTypeForField e) {
            throw e;
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T internalLoadDomContents(final Element node,
                                                 final Class<T> classs) throws NoSuchMethodException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Constructor<T> constructor;
        constructor = classs.getDeclaredConstructor(new Class[0]);
        constructor.setAccessible(true);
        final T newInstance = constructor.newInstance(new Object[0]);

        if (classs.getAnnotation(Selector.class) == null && classs.getAnnotation(Page.class) == null)
            return newInstance;

        final Field[] declaredFields = classs.getDeclaredFields();
        for (final Field f : declaredFields) {
            final Class<?> fieldClass = f.getType();

            if (fieldClass.equals(java.util.List.class) && f.getAnnotation(Selector.class) == null) {
                solveListOfAnnotatedType(node, newInstance, f);
            }

            if (f.getAnnotation(Selector.class) != null) {
                solveAnnotatedField(node, newInstance, f, fieldClass);
            }

            if (fieldClass.getAnnotation(Selector.class) != null) {
                solveUnanotatedFieldOfAnnotatedType(node, newInstance, f, fieldClass);
            }

        }
        return newInstance;
    }

    private static <T> void solveUnanotatedFieldOfAnnotatedType(final Element node, final T newInstance, final Field f, final Class<?> fieldClass) throws IllegalAccessException, InstantiationException {
        final String cssQuery = fieldClass.getAnnotation(Selector.class).value();
        final Element selectedNode = getFirstOrNullOrCryIfMoreThanOne(node, cssQuery);
        if(selectedNode == null) return;
        final Document innerHtml = Jsoup.parse(selectedNode.html());
        f.setAccessible(true);
        f.set(newInstance, loadDomContents(innerHtml, fieldClass));
    }

    private static Element getFirstOrNullOrCryIfMoreThanOne(final Element node, final String cssQuery) {
        final Elements elements = node.select(cssQuery);
        final int size = elements.size();
        if(size > 1){
            throw new TooManyResultsException(cssQuery, size);
        }
        if(size == 0){
            return null;
        }
        final Element selectedNode = elements.first();
        return selectedNode;
    }

    private static <T> void solveAnnotatedField(final Element node, final T newInstance, final Field f, final Class<?> fieldClass) throws IllegalAccessException, InstantiationException {
        if (fieldClass.equals(java.util.List.class)) {
            solveAnnotatedListField(node, newInstance, f);
        } else {
            solveAnnotatedFieldWithMappableType(node, newInstance, f, fieldClass);
        }
    }

    private static <T> void solveAnnotatedFieldWithMappableType(final Element node, final T newInstance, final Field f, final Class<?> fieldClass) throws IllegalAccessException {
        final Selector selectorAnnotation = f.getAnnotation(Selector.class);
        final String cssQuery = selectorAnnotation.value();
        final Element selectedNode = getFirstOrNullOrCryIfMoreThanOne(node, cssQuery);
        if(selectedNode == null) return;

        if (Instantiator.typeIsVisitable(fieldClass)) {
            final Class<?> visitableGenericClass = TypeToken.of(f.getGenericType()).resolveType(Link.class.getTypeParameters()[0]).getRawType();
            f.setAccessible(true);
            f.set(newInstance, Instantiator.visitableForNode(selectedNode, visitableGenericClass, Browser.currentPageUrl));
        }else{
            if (typeIsKnown(fieldClass)) {
                final String attribute = selectorAnnotation.attr();
                f.setAccessible(true);
                f.set(newInstance, instanceForNode(selectedNode, attribute, fieldClass));
            } else {
                throw new RuntimeException("Can't convert html to class " + fieldClass.getName() + "\n" +
                        "The field type must be a class with "+Page.class.getSimpleName()+" annotation or one of these types:\n" +
                        List.class.getCanonicalName()+"\n"+
                        String.class.getCanonicalName()+"\n"+
                        Integer.class.getCanonicalName()+"\n"+
                        Float.class.getCanonicalName()+"\n"+
                        Boolean.class.getCanonicalName()+"\n"+
                        Link.class.getCanonicalName()+"\n"+
                        Element.class.getCanonicalName()+"\n"
                );
            }
        }
    }

    private static <T> void solveAnnotatedListField(final Element node, final T newInstance, final Field f) throws IllegalAccessException, InstantiationException {
        final Type genericType = f.getGenericType();
        final String cssQuery = f.getAnnotation(Selector.class).value();
        final String attribute = f.getAnnotation(Selector.class).attr();
        final Elements nodes = node.select(cssQuery);

        Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if(type instanceof ParameterizedType){
            f.setAccessible(true);
            f.set(newInstance, populateListOfLinks(nodes, attribute, (ParameterizedType)type));
        }else{
            final Class<?> listClass = (Class<?>) type;
            f.setAccessible(true);
            f.set(newInstance, populateList(nodes, attribute, listClass));
        }
    }

    private static <T> void solveListOfAnnotatedType(final Element node, final T newInstance, final Field f) throws IllegalAccessException, InstantiationException {
        final Type genericType = f.getGenericType();
        final Class<?> listClass = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];

        final Selector selectorAnnotation = listClass.getAnnotation(Selector.class);
        if (selectorAnnotation != null) {
            final String cssQuery = selectorAnnotation.value();
            final String attribute = selectorAnnotation.attr();
            final Elements nodes = node.select(cssQuery);
            f.setAccessible(true);
            f.set(newInstance, populateList(nodes, attribute, listClass));
        }
    }

    private static <T> List<T> populateList(final Elements nodes, String attribute, final Class<T> classs) throws InstantiationException, IllegalAccessException {
        final ArrayList<T> newInstanceList = new ArrayList<T>();
        final Iterator<Element> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            final Element node = iterator.next();
            if (typeIsKnown(classs)) {
                newInstanceList.add(instanceForNode(node,attribute, classs));
            } else {
                newInstanceList.add(loadDomContents(node, classs));
            }
        }
        return newInstanceList;
    }

    private static <T> ArrayList<Link<T>> populateListOfLinks(final Elements nodes, String attribute, final ParameterizedType paraType) throws InstantiationException, IllegalAccessException {
        final ArrayList<Link<T>> newInstanceList = new ArrayList<Link<T>>();
        final Iterator<Element> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            final Element node = iterator.next();
            Class<?> classs = (Class<?>) paraType.getActualTypeArguments()[0];
            @SuppressWarnings("unchecked")
            Link<T> link = (Link<T>) Instantiator.visitableForNode(node, classs, Browser.currentPageUrl);
            newInstanceList.add(link);
        }
        return newInstanceList;
    }


    public static String getCurrentPageUrl() {
        return currentPageUrl;
    }

    public static String getCurrentPageContents() {
        return currentPageContents;
    }

    public static BrowserClient getClient() {
        return client;
    }

    public static void setClient(BrowserClient client) {
        Browser.client = client;
    }


}
