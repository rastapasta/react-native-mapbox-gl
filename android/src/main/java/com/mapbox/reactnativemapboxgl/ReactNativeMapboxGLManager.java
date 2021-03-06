
package com.mapbox.reactnativemapboxgl;

import android.util.Log;
import android.view.View;

import android.graphics.PointF;
import android.graphics.RectF;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.services.commons.geojson.Feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReactNativeMapboxGLManager extends ViewGroupManager<ReactNativeMapboxGLView> {

    private static final String REACT_CLASS = "RCTMapboxGL";

    private ReactApplicationContext _context;
    private Map<ReactNativeMapboxGLView, List<View>> _childViews;
    private Set<ChildListener> _childListeners;

    public ReactNativeMapboxGLManager(ReactApplicationContext context) {
        super();
        _context = context;
        _childViews = new HashMap<>();
        _childListeners = new HashSet<>();
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public ReactApplicationContext getContext() {
        return _context;
    }

    public List<RNMGLAnnotationView> getAnnotationViews(ReactNativeMapboxGLView parent) {
        List<RNMGLAnnotationView> annotationViews = new ArrayList<>();
        for (View view : _childViews.get(parent)) {
            if (RNMGLAnnotationView.class.equals(view.getClass())) {
                annotationViews.add((RNMGLAnnotationView) view);
            }
        }
        return annotationViews;
    }

    // Lifecycle methods

    @Override
    public ReactNativeMapboxGLView createViewInstance(ThemedReactContext context) {
        return new ReactNativeMapboxGLView(context, this);
    }

    @Override
    protected void onAfterUpdateTransaction(ReactNativeMapboxGLView view) {
        super.onAfterUpdateTransaction(view);
        view.onAfterUpdateTransaction();
    }

    @Override
    public void onDropViewInstance(ReactNativeMapboxGLView view) {
        view.onDrop();
    }

    // Event types
    @Override
    public @Nullable Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String,Object>builder()
                .put(ReactNativeMapboxGLEventTypes.ON_REGION_DID_CHANGE, MapBuilder.of("registrationName", "onRegionDidChange"))
                .put(ReactNativeMapboxGLEventTypes.ON_REGION_WILL_CHANGE, MapBuilder.of("registrationName", "onRegionWillChange"))
                .put(ReactNativeMapboxGLEventTypes.ON_OPEN_ANNOTATION, MapBuilder.of("registrationName", "onOpenAnnotation"))
                .put(ReactNativeMapboxGLEventTypes.ON_RIGHT_ANNOTATION_TAPPED, MapBuilder.of("registrationName", "onRightAnnotationTapped"))
                .put(ReactNativeMapboxGLEventTypes.ON_CHANGE_USER_TRACKING_MODE, MapBuilder.of("registrationName", "onChangeUserTrackingMode"))
                .put(ReactNativeMapboxGLEventTypes.ON_UPDATE_USER_LOCATION, MapBuilder.of("registrationName", "onUpdateUserLocation"))
                .put(ReactNativeMapboxGLEventTypes.ON_LONG_PRESS, MapBuilder.of("registrationName", "onLongPress"))
                .put(ReactNativeMapboxGLEventTypes.ON_TAP, MapBuilder.of("registrationName", "onTap"))
                .put(ReactNativeMapboxGLEventTypes.ON_FINISH_LOADING_MAP, MapBuilder.of("registrationName", "onFinishLoadingMap"))
                .put(ReactNativeMapboxGLEventTypes.ON_START_LOADING_MAP, MapBuilder.of("registrationName", "onStartLoadingMap"))
                .put(ReactNativeMapboxGLEventTypes.ON_LOCATE_USER_FAILED, MapBuilder.of("registrationName", "onLocateUserFailed"))
                .build();
    }

    // Children

    public interface ChildListener {
        void childAdded(View child);
        void childRemoved(View child);
    }

    public void addChildListener(ChildListener listener) {
        _childListeners.add(listener);
    }

    public void removeChildListener(ChildListener listener) {
        _childListeners.remove(listener);
    }

    @Override
    public void addView(ReactNativeMapboxGLView parent, View child, int index) {
        if (!_childViews.containsKey(parent)) {
            _childViews.put(parent, new ArrayList<View>());
        }
        _childViews.get(parent).add(index, child);
        if (!RNMGLAnnotationView.class.equals(child.getClass())) {
            super.addView(parent, child, getRealIndex(parent, index));
        }
        for (ChildListener listener : _childListeners) {
            listener.childAdded(child);
        }
    }

    @Override
    public int getChildCount(ReactNativeMapboxGLView parent) {
        return _childViews.get(parent).size();
    }

    @Override
    public View getChildAt(ReactNativeMapboxGLView parent, int index) {
        return _childViews.get(parent).get(index);
    }

    @Override
    public void removeViewAt(ReactNativeMapboxGLView parent, int index) {
        int realIndex = getRealIndex(parent, index);
        View child = _childViews.get(parent).remove(index);
        if (!RNMGLAnnotationView.class.equals(child.getClass())) {
            super.removeViewAt(parent, realIndex);
        }
        for (ChildListener listener : _childListeners) {
            listener.childRemoved(child);
        }
        if (_childViews.get(parent).isEmpty()) {
            _childViews.remove(parent);
        }
    }

    private int getRealIndex(ReactNativeMapboxGLView parent, int index) {
        int annotationViews = 0;
        for (int i = 0; i < index; i++) {
            if (RNMGLAnnotationView.class.equals(getChildAt(parent, i).getClass())) {
                annotationViews++;
            }
        }
        return index - annotationViews;
    }

    // Props

    @ReactProp(name = "initialZoomLevel")
    public void setInitialZoomLevel(ReactNativeMapboxGLView view, double value) {
        view.setInitialZoomLevel(value);
    }

    @ReactProp(name = "minimumZoomLevel")
    public void setMinumumZoomLevel(ReactNativeMapboxGLView view, double value) {
        view.setMinimumZoomLevel(value);
    }

    @ReactProp(name = "maximumZoomLevel")
    public void setMaxumumZoomLevel(ReactNativeMapboxGLView view, double value) {
        view.setMaximumZoomLevel(value);
    }

    @ReactProp(name = "initialDirection")
    public void setInitialDirection(ReactNativeMapboxGLView view, double value) {
        view.setInitialDirection(value);
    }

    @ReactProp(name = "initialCenterCoordinate")
    public void setInitialCenterCoordinate(ReactNativeMapboxGLView view, ReadableMap coord) {
        double lat = coord.getDouble("latitude");
        double lon = coord.getDouble("longitude");
        view.setInitialCenterCoordinate(lat, lon);
    }

    @ReactProp(name = "enableOnRegionDidChange")
    public void setEnableOnRegionDidChange(ReactNativeMapboxGLView view, boolean value) {
        view.setEnableOnRegionDidChange(value);
    }

    @ReactProp(name = "enableOnRegionWillChange")
    public void setEnableOnRegionWillChange(ReactNativeMapboxGLView view, boolean value) {
        view.setEnableOnRegionWillChange(value);
    }

    @ReactProp(name = "debugActive")
    public void setDebugActive(ReactNativeMapboxGLView view, boolean value) {
        view.setDebugActive(value);
    }

    @ReactProp(name = "rotateEnabled")
    public void setRotateEnabled(ReactNativeMapboxGLView view, boolean value) {
        view.setRotateEnabled(value);
    }

    @ReactProp(name = "scrollEnabled")
    public void setScrollEnabled(ReactNativeMapboxGLView view, boolean value) {
        view.setScrollEnabled(value);
    }

    @ReactProp(name = "zoomEnabled")
    public void setZoomEnabled(ReactNativeMapboxGLView view, boolean value) {
        view.setZoomEnabled(value);
    }

    @ReactProp(name = "pitchEnabled")
    public void setPitchEnabled(ReactNativeMapboxGLView view, boolean value) {
        view.setPitchEnabled(value);
    }

    @ReactProp(name = "annotationsPopUpEnabled")
    public void setAnnotationsPopUpEnabled(ReactNativeMapboxGLView view, boolean value) {
        view.setAnnotationsPopUpEnabled(value);
    }

    @ReactProp(name = "showsUserLocation")
    public void setShowsUserLocation(ReactNativeMapboxGLView view, boolean value) {
        view.setShowsUserLocation(value);
    }

    @ReactProp(name = "styleURL")
    public void setStyleUrl(ReactNativeMapboxGLView view, @Nonnull String styleURL) {
        view.setStyleURL(styleURL);
    }

    @ReactProp(name = "userTrackingMode")
    public void setUserTrackingMode(ReactNativeMapboxGLView view, int mode) {
        view.setLocationTracking(ReactNativeMapboxGLModule.locationTrackingModes[mode]);
        view.setBearingTracking(ReactNativeMapboxGLModule.bearingTrackingModes[mode]);
    }

    @ReactProp(name = "attributionButtonIsHidden")
    public void setAttributionButtonIsHidden(ReactNativeMapboxGLView view, boolean value) {
        view.setAttributionButtonIsHidden(value);
    }

    @ReactProp(name = "logoIsHidden")
    public void setLogoIsHidden(ReactNativeMapboxGLView view, boolean value) {
        view.setLogoIsHidden(value);
    }

    @ReactProp(name = "compassIsHidden")
    public void setCompassIsHidden(ReactNativeMapboxGLView view, boolean value) {
        view.setCompassIsHidden(value);
    }

    @ReactProp(name = "contentInset")
    public void setContentInset(ReactNativeMapboxGLView view, ReadableArray inset) {
        view.setContentInset(inset.getInt(0), inset.getInt(1), inset.getInt(2), inset.getInt(3));
    }

    // Commands

    public static final int COMMAND_GET_DIRECTION = 0;
    public static final int COMMAND_GET_PITCH = 1;
    public static final int COMMAND_GET_CENTER_COORDINATE_ZOOM_LEVEL = 2;
    public static final int COMMAND_GET_BOUNDS = 3;
    public static final int COMMAND_EASE_TO = 4;
    public static final int COMMAND_SET_VISIBLE_COORDINATE_BOUNDS = 6;
    public static final int COMMAND_SELECT_ANNOTATION = 7;
    public static final int COMMAND_SPLICE_ANNOTATIONS = 8;
    public static final int COMMAND_DESELECT_ANNOTATION = 9;
    public static final int COMMAND_QUERY_RENDERED_FEATURES = 10;

    @Override
    public
    @Nullable
    Map<String, Integer> getCommandsMap() {
        return MapBuilder.<String, Integer>builder()
                .put("getDirection", COMMAND_GET_DIRECTION)
                .put("getPitch", COMMAND_GET_PITCH)
                .put("getCenterCoordinateZoomLevel", COMMAND_GET_CENTER_COORDINATE_ZOOM_LEVEL)
                .put("getBounds", COMMAND_GET_BOUNDS)
                .put("easeTo", COMMAND_EASE_TO)
                .put("setVisibleCoordinateBounds", COMMAND_SET_VISIBLE_COORDINATE_BOUNDS)
                .put("selectAnnotation", COMMAND_SELECT_ANNOTATION)
                .put("spliceAnnotations", COMMAND_SPLICE_ANNOTATIONS)
                .put("deselectAnnotation", COMMAND_DESELECT_ANNOTATION)
                .put("queryRenderedFeatures", COMMAND_QUERY_RENDERED_FEATURES)
                .build();
    }

    private void fireCallback(int callbackId, WritableArray args) {
        WritableArray event = Arguments.createArray();
        event.pushInt(callbackId);
        event.pushArray(args);

        _context.getJSModule(RCTNativeAppEventEmitter.class)
                .emit("MapboxAndroidCallback", event);
    }

    @Override
    public void receiveCommand(ReactNativeMapboxGLView view, int commandId, @Nullable ReadableArray args) {
        Assertions.assertNotNull(args);
        switch (commandId) {
            case COMMAND_GET_DIRECTION:
                getDirection(view, args.getInt(0));
                break;
            case COMMAND_GET_PITCH:
                getPitch(view, args.getInt(0));
                break;
            case COMMAND_GET_CENTER_COORDINATE_ZOOM_LEVEL:
                getCenterCoordinateZoomLevel(view, args.getInt(0));
                break;
            case COMMAND_GET_BOUNDS:
                getBounds(view, args.getInt(0));
                break;
            case COMMAND_EASE_TO:
                easeTo(view, args.getMap(0), args.getBoolean(1), args.getInt(2));
                break;
            case COMMAND_SET_VISIBLE_COORDINATE_BOUNDS:
                setVisibleCoordinateBounds(view,
                        args.getDouble(0), args.getDouble(1), args.getDouble(2), args.getDouble(3),
                        args.getDouble(4), args.getDouble(5), args.getDouble(6), args.getDouble(7),
                        args.getBoolean(8)
                );
                break;
            case COMMAND_SELECT_ANNOTATION:
                selectAnnotation(view, args.getString(0), args.getBoolean(1));
                break;
            case COMMAND_SPLICE_ANNOTATIONS:
                spliceAnnotations(view, args.getBoolean(0), args.getArray(1), args.getArray(2));
                break;
            case COMMAND_DESELECT_ANNOTATION:
                deselectAnnotation(view);
                break;

            case COMMAND_QUERY_RENDERED_FEATURES:
                queryRenderedFeatures(view, args.getMap(0), args.getInt(1));
                break;
            default:
                throw new JSApplicationIllegalArgumentException("Invalid commandId " + commandId + " sent to " + getClass().getSimpleName());
        }
    }

    // Getters

    private void getDirection(ReactNativeMapboxGLView view, int callbackId) {
        WritableArray result = Arguments.createArray();
        result.pushDouble(view.getCameraPosition().bearing);
        fireCallback(callbackId, result);
    }

    private void getPitch(ReactNativeMapboxGLView view, int callbackId) {
        WritableArray result = Arguments.createArray();
        result.pushDouble(view.getCameraPosition().tilt);
        fireCallback(callbackId, result);
    }

    private void getCenterCoordinateZoomLevel(ReactNativeMapboxGLView view, int callbackId) {
        CameraPosition camera = view.getCameraPosition();

        WritableArray args = Arguments.createArray();
        WritableMap result = Arguments.createMap();
        result.putDouble("latitude", camera.target.getLatitude());
        result.putDouble("longitude", camera.target.getLongitude());
        result.putDouble("zoomLevel", camera.zoom);
        args.pushMap(result);

        fireCallback(callbackId, args);
    }

    private void getBounds(ReactNativeMapboxGLView view, int callbackId) {
        LatLngBounds bounds = view.getBounds();

        WritableArray args = Arguments.createArray();
        WritableArray result = Arguments.createArray();
        result.pushDouble(bounds.getLatSouth());
        result.pushDouble(bounds.getLonWest());
        result.pushDouble(bounds.getLatNorth());
        result.pushDouble(bounds.getLonEast());
        args.pushArray(result);

        fireCallback(callbackId, args);
    }

    // Setters

    private void easeTo(ReactNativeMapboxGLView view, ReadableMap updates, boolean animated, int callbackId) {
        CameraPosition oldPosition = view.getCameraPosition();
        CameraPosition.Builder cameraBuilder = new CameraPosition.Builder(oldPosition);

        if (updates.hasKey("latitude") && updates.hasKey("longitude")) {
            cameraBuilder.target(new LatLng(updates.getDouble("latitude"), updates.getDouble("longitude")));
        }
        if (updates.hasKey("zoomLevel")) {
            cameraBuilder.zoom(updates.getDouble("zoomLevel"));
        }
        if (updates.hasKey("direction")) {
            cameraBuilder.bearing(updates.getDouble("direction"));
        }
        if (updates.hasKey("pitch")) {
            cameraBuilder.tilt(updates.getDouble("pitch"));
        }

        // I want lambdas :(
        class CallbackRunnable implements Runnable {
            int callbackId;
            ReactNativeMapboxGLManager manager;

            CallbackRunnable(ReactNativeMapboxGLManager manager, int callbackId) {
                this.callbackId = callbackId;
                this.manager = manager;
            }

            @Override
            public void run() {
                manager.fireCallback(callbackId, Arguments.createArray());
            }
        }

        int duration = animated ? MapboxConstants.ANIMATION_DURATION : 0;
        view.setCameraPosition(cameraBuilder.build(), duration, new CallbackRunnable(this, callbackId));
    }

    public void setCamera(
            ReactNativeMapboxGLView view,
            double latitude, double longitude,
            double altitude, double pitch, double direction,
            double duration) {
        throw new JSApplicationIllegalArgumentException("MapView.setCamera() is not supported on Android. If you're trying to change pitch, use MapView.easeTo()");
    }

    public void setVisibleCoordinateBounds(
            ReactNativeMapboxGLView view,
            double latS, double lonW, double latN, double lonE,
            double paddingTop, double paddingRight, double paddingBottom, double paddingLeft,
            boolean animated) {
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
                new LatLngBounds.Builder()
                        .include(new LatLng(latS, lonW))
                        .include(new LatLng(latN, lonE))
                        .build(),
                (int) paddingLeft,
                (int) paddingTop,
                (int) paddingRight,
                (int) paddingBottom
        );
        view.setCameraUpdate(update, animated ? MapboxConstants.ANIMATION_DURATION : 0, null);
    }

    // Annotations

    public void spliceAnnotations(ReactNativeMapboxGLView view, boolean removeAll, ReadableArray itemsToRemove, ReadableArray itemsToAdd) {
        if (removeAll) {
            view.removeAllAnnotations();
        } else {
            int removeCount = itemsToRemove.size();
            for (int i = 0; i < removeCount; i++) {
                view.removeAnnotation(itemsToRemove.getString(i));
            }
        }

        int addCount = itemsToAdd.size();
        for (int i = 0; i < addCount; i++) {
            ReadableMap annotation = itemsToAdd.getMap(i);
            RNMGLAnnotationOptions annotationOptions = RNMGLAnnotationOptionsFactory.annotationOptionsFromJS(annotation, view.getContext());

            String name = annotation.getString("id");
            view.setAnnotation(name, annotationOptions);
        }
    }

    public void selectAnnotation(ReactNativeMapboxGLView view, String annotationId, boolean animated) {
        view.selectAnnotation(annotationId, animated);
    }

    public void deselectAnnotation(ReactNativeMapboxGLView view) {
        view.deselectAnnotation();
    }

    public void queryRenderedFeatures(ReactNativeMapboxGLView view, ReadableMap options, int callbackId) {
        WritableArray callbackArgs = Arguments.createArray();
        if ((!options.hasKey("point") && !options.hasKey("rect")) || (options.hasKey("point") && options.hasKey("rect"))) {
            callbackArgs.pushString("queryRenderedFeatures(): one of 'point' or 'rect' is required.");
            fireCallback(callbackId, callbackArgs);
            return;
        }

        String[]layers = null;
        if (options.hasKey("layers")) {
        ReadableArray layersArray = options.getArray("layers");
        layers = new String[layersArray.size()];
        for (int i = 0; i < layersArray.size(); i++) {
            String layerName = layersArray.getString(i);
            layers[i] = layerName;
        }
        }

        List<Feature> featuresList = null;
        if (options.hasKey("point")) {
        ReadableMap pointMap = options.getMap("point");
        float screenCoordX = (float)pointMap.getDouble("screenCoordX");
        float screenCoordY = (float)pointMap.getDouble("screenCoordY");
        PointF point = new PointF(screenCoordX, screenCoordY);
        featuresList = view.queryRenderedFeatures(point, layers);
        } else {
        ReadableMap rectMap = options.getMap("rect");
        float left = (float)rectMap.getDouble("left");
        float top = (float)rectMap.getDouble("top");
        float right = (float)rectMap.getDouble("right");
        float bottom = (float)rectMap.getDouble("bottom");
        RectF rect = new RectF(left, top, right, bottom);
        featuresList = view.queryRenderedFeatures(rect, layers);
        }

        WritableArray jsonFeatures = Arguments.createArray();
        for (int i = 0; i < featuresList.size(); i++) {
        Feature feature = featuresList.get(i);
        jsonFeatures.pushString(feature.toJson());
        }

        callbackArgs.pushString(null); // push null error message
        callbackArgs.pushArray(jsonFeatures); // second arg is features GeoJSON
        fireCallback(callbackId, callbackArgs);
    }
}
