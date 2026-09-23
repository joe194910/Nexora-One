export const NexoraLoading = {
  show: function (msg) {
    uni.showLoading({ title: msg ? msg : '加载中' });
  },

  hide: function () {
    uni.hideLoading();
  },
};

export const NexoraToast = {
  success: (message) => {
    uni.showToast({
      title: message,
      icon: 'success',
    });
  },
  error: (message) => {
    uni.showToast({
      title: message,
      icon: 'error',
    });
  },
  toast: (message) => {
    uni.showToast({
      title: message,
      icon: 'none',
    });
  },
};
