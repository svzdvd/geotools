package org.geotools.data.wfs.v1_0_0.demo;

import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;


public class TinyOWSDemo {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(new FileInputStream("logging.properties"));
        
        URL url = new URL("http://127.0.0.1:8888/cgi-bin/tinyows?service=WFS&version=1.1.0&REQUEST=GetCapabilities");

        WFSDataStoreFactory wfsDataStoreFactory = new WFSDataStoreFactory();
        
        Map params = new HashMap();
        params.put(WFSDataStoreFactory.URL.key, url);
        params.put(WFSDataStoreFactory.WFS_STRATEGY.key, "tinyows");
        // not debug:
        params.put(WFSDataStoreFactory.TIMEOUT.key, new Integer(10000));
        // for debug:
        // params.put(WFSDataStoreFactory.TIMEOUT.key,new Integer(1000000));
        
        WFSDataStore wfs = wfsDataStoreFactory.createDataStore(params);
        
        // this doesn't work with WFS_1_0_0_DataStore
        wfs.setPreferPostOverGet(true);
        
        String types[] = wfs.getTypeNames();
        for (int i = 0; i < types.length; i++) {
            String typeName = types[i];            
            System.out.println("found type: " + typeName);
        }
        
        String typeName = "comuni:comuni11";
        SimpleFeatureSource source = wfs.getFeatureSource(typeName);
        
        Query query = new Query(typeName, Filter.INCLUDE, 20, Query.ALL_NAMES, "my query");
        // TODO this doesn't work: query.setSortBy(new SortBy[] { SortBy.NATURAL_ORDER });
        SimpleFeature sf = iterate("query with 20 features limit", source.getFeatures(query), 20);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Set<FeatureId> fids = new HashSet<FeatureId>();
        fids.add(new FeatureIdImpl("comuni11.2671")); // sf.getIdentifier());
        query = new Query(typeName, ff.id(fids));
        sf = iterate("query by Id", source.getFeatures(query), 1);
        
        PropertyName bboxProperty = ff.property(sf.getDefaultGeometryProperty().getName());
        query = new Query(typeName, ff.bbox(bboxProperty, sf.getBounds()));
        iterate("query by BBOX", source.getFeatures(query), 6);
                
        query = new Query(typeName, ff.and(ff.id(fids), 
                ff.bbox(bboxProperty, sf.getBounds())));
        iterate("query by Id and BBOX", source.getFeatures(query), 1);

        query = new Query(typeName, ff.and(
                ff.bbox(bboxProperty, sf.getBounds()),
                ff.id(fids)));
        iterate("query by BBOX and Id", source.getFeatures(query), 1);
        
        query = new Query(typeName, ff.and(
                ff.greater(ff.property("gid"), ff.literal(0)), 
                ff.and(
                        ff.bbox(bboxProperty, sf.getBounds()),
                        ff.id(fids))));        
        iterate("gid > 0 AND (bbox AND ids)", source.getFeatures(query), 1);        
        
        query = new Query(typeName, ff.and(
                ff.bbox(bboxProperty, sf.getBounds()), 
                ff.or(
                        ff.bbox(bboxProperty, sf.getBounds()),
                        ff.id(fids))));        
        iterate("bbox AND (bbox OR ids)", source.getFeatures(query), 6);        
    }
    
    private static SimpleFeature iterate(String name, SimpleFeatureCollection features, int expectedSize) {
        System.out.println(name + " - count: " + features.size());
        if (features.size() > -1) {
            if (features.size() == expectedSize) {
                System.out.println("SIZE OK");
            } else {
                System.out.println("FAILED. Expected " + expectedSize + " but was " + features.size());
                System.exit(-1);
            }
        }
        
        int size = 0;
        SimpleFeatureIterator reader = features.features();
        SimpleFeature sf = null;
        try {
            while (reader.hasNext()) {
                if (sf == null) {
                    sf = reader.next();
                    System.out.println("found: " + sf.getIdentifier());
                } else {
                    System.out.println("found: " + reader.next().getIdentifier());
                }     
                size++;
            }
        } finally {
            reader.close();
        }  
        
        if (size == expectedSize) {
            System.out.println("SIZE OK");
        } else {
            System.out.println("FAILED. Expected " + expectedSize + " but was " + size);                
            System.exit(-1);            
        }        
        
        return sf;
    }
}