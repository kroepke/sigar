#ifdef WIN32
#include <pdh.h>
#include <pdhmsg.h>

#include "win32bindings.h"
#include "javasigar.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Hack around not being able to format error codes using
 * FORMAT_MESSAGE_FROM_HMODULE.  We only define the error
 * codes that could be possibly returned.
 */
static char *get_error_message(PDH_STATUS status) {
    switch (status) {
    case PDH_CSTATUS_NO_MACHINE:
        return "The computer is unavailable";
    case PDH_CSTATUS_NO_OBJECT:
        return "The specified object could not be found on the computer";
    case PDH_INVALID_ARGUMENT:
        return "A required argument is invalid";
    case PDH_MEMORY_ALLOCATION_FAILURE:
        return "A required temporary buffer could not be allocated";
    case PDH_INVALID_HANDLE:
        return "The query handle is not valid";
    case PDH_NO_DATA:
        return "The query does not currently have any counters";
    case PDH_CSTATUS_BAD_COUNTERNAME:
        return "The counter name path string could not be parsed or "
            "interpreted";
    case PDH_CSTATUS_NO_COUNTER:
        return "The specified counter was not found";
    case PDH_CSTATUS_NO_COUNTERNAME:
        return "An empty counter name path string was passed in";
    case PDH_FUNCTION_NOT_FOUND:
        return "The calculation function for this counter could not "
            "be determined";
    default:
        return "Unknown error";
    }
}

JNIEXPORT jlong JNICALL SIGAR_JNI(win32_Pdh_pdhOpenQuery)
(JNIEnv *env, jobject cur)
{
    HQUERY     h_query;
    PDH_STATUS status;

    status = PdhOpenQuery(NULL, 0, &h_query);
    if (status != ERROR_SUCCESS) {
        win32_throw_exception(env, get_error_message(status));
        return 0;
    }
    return (jlong)h_query;
}

JNIEXPORT void SIGAR_JNI(win32_Pdh_pdhCloseQuery)
(JNIEnv *env, jclass cur, jlong query)
{
    HQUERY     h_query    = (HQUERY)query;
    PDH_STATUS status;

    // Close the query and the log file.
    status = PdhCloseQuery(h_query);

    if (status != ERROR_SUCCESS) {
        win32_throw_exception(env, get_error_message(status));
        return;
    }
}

JNIEXPORT jlong SIGAR_JNI(win32_Pdh_pdhAddCounter)
(JNIEnv *env, jclass cur, jlong query, jstring cp)
{
    HCOUNTER   h_counter;
    HQUERY     h_query = (HQUERY)query;
    PDH_STATUS status;
    LPCTSTR    counter_path = JENV->GetStringUTFChars(env, cp, NULL);

    /* Add the counter that created the data in the log file. */
    status = PdhAddCounter(h_query, counter_path, 0, &h_counter);
    JENV->ReleaseStringUTFChars(env, cp, counter_path);

    if (status != ERROR_SUCCESS) {
        win32_throw_exception(env, get_error_message(status));
        return 0;
    }

    return (jlong)h_counter;
}

JNIEXPORT void SIGAR_JNI(win32_Pdh_pdhRemoveCounter)
(JNIEnv *env, jclass cur, jlong counter)
{
    HCOUNTER   h_counter = (HCOUNTER)counter;
    PDH_STATUS status;
    
    status = PdhRemoveCounter(h_counter);

    if (status != ERROR_SUCCESS) {
        win32_throw_exception(env, get_error_message(status));
        return;
    }
}

JNIEXPORT jdouble SIGAR_JNI(win32_Pdh_pdhGetSingleValue)
(JNIEnv *env, jclass cur, jlong query, jlong counter)
{
    HCOUNTER              h_counter      = (HCOUNTER)counter;
    HQUERY                h_query        = (HQUERY)query;
    PDH_FMT_COUNTERVALUE  pdh_value;
    PDH_STATUS            status;

    status = PdhCollectQueryData(h_query);
   
    if (status != ERROR_SUCCESS) {
        win32_throw_exception(env, get_error_message(status));
        return 0;
    }

    // Format the performance data record.
    status = PdhGetFormattedCounterValue(h_counter,
                                         PDH_FMT_DOUBLE,
                                         (LPDWORD)NULL,
                                         &pdh_value);

    if (status != ERROR_SUCCESS) {
        win32_throw_exception(env, get_error_message(status));
        return 0;
    }

    return pdh_value.doubleValue;
}

JNIEXPORT jobjectArray SIGAR_JNI(win32_Pdh_pdhGetInstances)
(JNIEnv *env, jclass cur, jstring cp)
{
    PDH_STATUS   status              = ERROR_SUCCESS;
    DWORD        counter_list_size   = 0;
    DWORD        instance_list_size  = 8096;
    LPTSTR       instance_list_buf   = 
        (LPTSTR)malloc ((instance_list_size * sizeof (TCHAR)));
    LPTSTR       cur_object          = NULL;
    LPCTSTR      counter_path        = 
        (LPCTSTR)JENV->GetStringUTFChars(env, cp, 0);
    jobjectArray array = NULL;

    status = PdhEnumObjectItems(NULL, NULL, counter_path, NULL,
                                &counter_list_size, instance_list_buf,
                                &instance_list_size, PERF_DETAIL_WIZARD,
                                FALSE);
    
    if (status == PDH_MORE_DATA && instance_list_size > 0) {
        // Allocate the buffers and try the call again.
        if (instance_list_buf != NULL) 
            free(instance_list_buf);
        
        instance_list_buf = (LPTSTR)malloc((instance_list_size * 
                                            sizeof (TCHAR)));
        counter_list_size = 0;
        status  = PdhEnumObjectItems (NULL, NULL, counter_path,
                                      NULL, &counter_list_size,
                                      instance_list_buf,
                                      &instance_list_size,
                                      PERF_DETAIL_WIZARD, FALSE);
    }

    JENV->ReleaseStringUTFChars(env, cp, counter_path);

    // Still may get PDH_ERROR_MORE data after the first reallocation,
    // but that is OK for just browsing the instances
    if (status == ERROR_SUCCESS || status == PDH_MORE_DATA) {
        int i, count;
        
        for (cur_object = instance_list_buf, count = 0;
             *cur_object != 0;
             cur_object += lstrlen(cur_object) + 1, count++);
            
        array = JENV->NewObjectArray(env, count,
                                     JENV->FindClass(env, 
                                                     "java/lang/String"),
                                     JENV->NewStringUTF(env, ""));

        /* Walk the return instance list, creating an array */
        for (cur_object = instance_list_buf, i = 0;
             *cur_object != 0;
             cur_object += lstrlen(cur_object) + 1, i++) 
        {
            jstring s = JENV->NewStringUTF(env, cur_object);
            JENV->SetObjectArrayElement(env, array, i, s);
        }
    } else {
        if (instance_list_buf != NULL) 
            free(instance_list_buf);
        
        // An error occured
        win32_throw_exception(env, get_error_message(status));
        return NULL;
    }

    if (instance_list_buf != NULL) 
        free(instance_list_buf);

    return array;
}

JNIEXPORT jobjectArray SIGAR_JNI(win32_Pdh_pdhGetKeys)
(JNIEnv *env, jclass cur, jstring cp)
{
    PDH_STATUS  status              = ERROR_SUCCESS;
    DWORD       counter_list_size   = 8096;
    DWORD       instance_list_size  = 0;
    LPTSTR      instance_list_buf   = 
        (LPTSTR)malloc (counter_list_size * sizeof(TCHAR));
    LPTSTR      cur_object          = NULL;
    LPCTSTR     counter_path        = JENV->GetStringUTFChars(env, cp, 0);
    jobjectArray array              = NULL;

    status = PdhEnumObjectItems(NULL, NULL, counter_path,
                                instance_list_buf, &counter_list_size,
                                NULL, &instance_list_size,
                                PERF_DETAIL_WIZARD, FALSE); 
        
    if (status == PDH_MORE_DATA) {
        /* Allocate the buffers and try the call again. */
        if (instance_list_buf != NULL)
            free(instance_list_buf);

        instance_list_buf = (LPTSTR)malloc(counter_list_size *
                                           sizeof(TCHAR));
        instance_list_size = 0;
        status = PdhEnumObjectItems (NULL, NULL, counter_path,
                                     instance_list_buf,
                                     &counter_list_size, NULL,
                                     &instance_list_size,
                                     PERF_DETAIL_WIZARD, 0);
    }

    JENV->ReleaseStringUTFChars(env, cp, counter_path);

    if (status == ERROR_SUCCESS || status == PDH_MORE_DATA) {
        int i, count;

        for (cur_object = instance_list_buf, count = 0;
             *cur_object != 0;
             cur_object += lstrlen(cur_object) + 1, count++);

        array = JENV->NewObjectArray(env, count,
                                     JENV->FindClass(env, 
                                                     "java/lang/String"),
                                     JENV->NewStringUTF(env, ""));

        /* Walk the return instance list, creating an array */
        for (cur_object = instance_list_buf, i = 0;
             *cur_object != 0;
             cur_object += lstrlen(cur_object) + 1, i++) 
        {
            jstring s = JENV->NewStringUTF(env, cur_object);
            JENV->SetObjectArrayElement(env, array, i, s);
        }
    } else {
        // An error occured
        if (instance_list_buf != NULL) 
            free(instance_list_buf);
        
        // An error occured
        win32_throw_exception(env, get_error_message(status));
        return NULL;
    }
    
    if (instance_list_buf != NULL) 
            free(instance_list_buf);

    return array;
}

JNIEXPORT jobjectArray SIGAR_JNI(win32_Pdh_pdhGetObjects)
(JNIEnv *env, jclass cur)
{
    PDH_STATUS   status;
    DWORD        list_size       = 8096;
    LPTSTR       list_buf        = (LPTSTR)malloc(list_size * sizeof(TCHAR));
    LPTSTR       cur_object;
    DWORD        i, num_objects  = 0;
    jobjectArray array           = NULL;

    status = PdhEnumObjects(NULL, NULL, list_buf, &list_size,
                            PERF_DETAIL_WIZARD, FALSE);

    if (status == PDH_MORE_DATA) {
        // Re-try call with a larger buffer
        if (list_buf != NULL)
            free(list_buf);

        list_buf = (LPTSTR)malloc(list_size * sizeof(TCHAR));
        status = PdhEnumObjects(NULL, NULL, list_buf, &list_size,
                                PERF_DETAIL_WIZARD, FALSE);
    }

    if (status != ERROR_SUCCESS) {
        if (list_buf != NULL)
            free(list_buf);

        win32_throw_exception(env, get_error_message(status));
        return NULL;
    }

    // Walk the return buffer counting the number of objects
    for (cur_object = list_buf, num_objects = 0;
         *cur_object != 0;
         cur_object += lstrlen(cur_object) + 1, num_objects++);

    array = JENV->NewObjectArray(env, num_objects,
                                 JENV->FindClass(env, 
                                                 "java/lang/String"),
                                 JENV->NewStringUTF(env, ""));

    for (cur_object = list_buf, i = 0;
         *cur_object != 0;
         cur_object += lstrlen(cur_object) + 1, i++) 
    {
        jstring s = JENV->NewStringUTF(env, cur_object);
        JENV->SetObjectArrayElement(env, array, i, s);
    }

    if (list_buf != NULL)
        free(list_buf);

    return array;
}

#ifdef __cplusplus
}
#endif
#endif /* WIN32 */