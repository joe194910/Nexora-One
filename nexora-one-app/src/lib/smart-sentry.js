/*
 * 错误上报sentry
 *
 * @Author:    NexoraOne-主任：卓大
 * @Date:      2024-01-02 20:49:28
 * @Wechat:    NexoraOne
 * @Email:     NexoraOne
 * @Copyright  NexoraOne （ # ），Since 2012
 */

export const smartSentry = {
    /**
     * sentry 主动上报
     */
    captureError: (error) => {
        if (error.config && error.data && error && error.headers && error.request && error.status) {
            return;
        }
        // Sentry.captureException(error);
        console.error(error);
    },
};
