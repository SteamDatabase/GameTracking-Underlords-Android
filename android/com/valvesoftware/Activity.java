package com.valvesoftware;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.Vector;
import org.libsdl.app.SDLActivity;

public class Activity extends android.app.Activity implements IActivity {
    private static final String k_sSpewPackageName = "com.valvesoftware.Activity";
    static Vector<WeakReference<android.app.Activity>> sm_Activities;

    public boolean IsLaunchActivity() {
        return false;
    }

    public static android.app.Activity GetInstallActivity() {
        IActivity cast;
        for (int i = 0; i < sm_Activities.size(); i++) {
            android.app.Activity activity = (android.app.Activity) sm_Activities.get(i).get();
            if (activity != null && (cast = IActivity.class.cast(activity)) != null && cast.IsLaunchActivity()) {
                return activity;
            }
        }
        return null;
    }

    public static android.app.Activity GetNewestActivity() {
        android.app.Activity activity;
        int size = sm_Activities.size();
        do {
            size--;
            if (size < 0) {
                return null;
            }
            activity = (android.app.Activity) sm_Activities.get(size).get();
        } while (activity == null);
        return activity;
    }

    public static android.app.Activity GetPurchaseActivity() {
        return GetNewestActivity();
    }

    public static void KillAllActivities() {
        int size = sm_Activities.size();
        while (true) {
            size--;
            if (size >= 0) {
                android.app.Activity activity = (android.app.Activity) sm_Activities.get(size).get();
                if (activity != null) {
                    activity.finishAndRemoveTask();
                }
            } else {
                return;
            }
        }
    }

    public static void RegisterActivityListener(Application application) {
        if (ActivityLifecycleListener.sm_Instance == null) {
            sm_Activities = new Vector<>();
            ActivityLifecycleListener.sm_Instance = new ActivityLifecycleListener();
            application.registerActivityLifecycleCallbacks(ActivityLifecycleListener.sm_Instance);
        }
    }

    private static class ActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {
        static ActivityLifecycleListener sm_Instance;

        public void onActivityPaused(android.app.Activity activity) {
        }

        public void onActivitySaveInstanceState(android.app.Activity activity, Bundle bundle) {
        }

        public void onActivityStarted(android.app.Activity activity) {
        }

        private ActivityLifecycleListener() {
        }

        public void onActivityCreated(android.app.Activity activity, Bundle bundle) {
            if (IActivity.class.isInstance(activity)) {
                String name = activity.getClass().getName();
                ComponentName callingActivity = activity.getCallingActivity();
                String flattenToString = callingActivity != null ? callingActivity.flattenToString() : "<none>";
                String intent = activity.getIntent().toString();
                Log.i(Activity.k_sSpewPackageName, "onActivityCreated() class:" + name + ", caller:" + flattenToString + ", intent:" + intent);
                Activity.sm_Activities.add(new WeakReference(activity));
                return;
            }
            throw new AssertionError("com.valvesoftware.Application.CastActivity() activity does not implement from com.valvesoftware.IActivity");
        }

        public void onActivityDestroyed(android.app.Activity activity) {
            String name = activity.getClass().getName();
            ComponentName callingActivity = activity.getCallingActivity();
            String flattenToString = callingActivity != null ? callingActivity.flattenToString() : "<none>";
            Log.i(Activity.k_sSpewPackageName, "onActivityDestroyed() class:" + name + ", caller:" + flattenToString + ", intent:" + activity.getIntent().toString());
            int i = 0;
            while (i < Activity.sm_Activities.size()) {
                android.app.Activity activity2 = (android.app.Activity) Activity.sm_Activities.get(i).get();
                if (activity2 == null) {
                    Activity.sm_Activities.removeElementAt(i);
                } else if (activity2 == activity) {
                    Activity.sm_Activities.removeElementAt(i);
                } else {
                    i++;
                }
                i--;
                i++;
            }
        }

        public void onActivityResumed(android.app.Activity activity) {
            String name = activity.getClass().getName();
            ComponentName callingActivity = activity.getCallingActivity();
            String flattenToString = callingActivity != null ? callingActivity.flattenToString() : "<none>";
            String intent = activity.getIntent().toString();
            Log.i(Activity.k_sSpewPackageName, "onActivityResumed() class:" + name + ", caller:" + flattenToString + ", intent:" + intent);
            if (IActivity.class.cast(activity).IsLaunchActivity()) {
                Application.GetInstance().ResumeInstall();
            }
        }

        public void onActivityStopped(android.app.Activity activity) {
            String name = activity.getClass().getName();
            ComponentName callingActivity = activity.getCallingActivity();
            String flattenToString = callingActivity != null ? callingActivity.flattenToString() : "<none>";
            String intent = activity.getIntent().toString();
            Log.i(Activity.k_sSpewPackageName, "onActivityStopped() class:" + name + ", caller:" + flattenToString + ", intent:" + intent);
            if (IActivity.class.cast(activity).IsLaunchActivity()) {
                Application.GetInstance().PauseInstall();
            }
        }
    }

    public static class SDLActivityWrapper extends SDLActivity implements IActivity {
        public boolean IsLaunchActivity() {
            return false;
        }

        public static class OverrideClassLoader extends ClassLoader {
            public OverrideClassLoader(ClassLoader classLoader) {
                super(classLoader);
            }

            private static class ReLinkerProxy {
                private ReLinkerProxy() {
                }

                public static Object force() {
                    Log.i(Activity.k_sSpewPackageName, "ReLinkerProxy::force()");
                    return new ReLinkerInstanceProxy();
                }
            }

            private static class ReLinkerInstanceProxy {
                private ReLinkerInstanceProxy() {
                }

                public ReLinkerInstanceProxy force() {
                    Log.i(Activity.k_sSpewPackageName, "ReLinkerInstanceProxy::force()");
                    return this;
                }

                public void loadLibrary(Context context, String str, String str2, ReLinkerListenerProxy reLinkerListenerProxy) {
                    Log.i(Activity.k_sSpewPackageName, "ReLinkerInstanceProxy::loadLibrary( \"" + str + "\", \"" + str2 + "\" )");
                    StringBuilder sb = new StringBuilder();
                    sb.append("lib");
                    sb.append(str);
                    sb.append(".so");
                    JNI_Environment.FindAndLoadNativeLibrary(sb.toString());
                }
            }

            private static class ReLinkerListenerProxy {
                private ReLinkerListenerProxy() {
                }
            }

            public Class<?> loadClass(String str) throws ClassNotFoundException {
                if (str.equals("com.getkeepsafe.relinker.ReLinker")) {
                    return ReLinkerProxy.class;
                }
                if (str.equals("com.getkeepsafe.relinker.ReLinker$LoadListener")) {
                    return ReLinkerListenerProxy.class;
                }
                return super.loadClass(str);
            }
        }

        public ClassLoader getClassLoader() {
            return new OverrideClassLoader(super.getClassLoader());
        }
    }
}
