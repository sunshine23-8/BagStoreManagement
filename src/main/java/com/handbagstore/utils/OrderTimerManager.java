package com.handbagstore.utils;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Quản lý timer cho đơn hàng PENDING.
 * Mỗi đơn hàng pending có 1 ScheduledFuture riêng.
 * Khi thanh toán / hủy thủ công → cancel task trước khi timeout.
 */
public class OrderTimerManager {
    private static OrderTimerManager instance;
    private final ScheduledExecutorService scheduler;
    private final Map<Integer, ScheduledFuture<?>> pendingTasks;

    private OrderTimerManager() {
        scheduler = Executors.newScheduledThreadPool(4);
        pendingTasks = new ConcurrentHashMap<>();
    }

    public static synchronized OrderTimerManager getInstance() {
        if (instance == null) {
            instance = new OrderTimerManager();
        }
        return instance;
    }

    /**
     * Lên lịch auto-cancel cho đơn hàng sau delayMs mili-giây.
     * @param invoiceId   ID hóa đơn pending
     * @param cancelTask  Runnable callback sẽ chạy khi hết hạn (cancel + release inventory)
     * @param delayMs     Thời gian chờ tính bằng mili-giây
     */
    public void scheduleCancelTask(int invoiceId, Runnable cancelTask, long delayMs) {
        // Hủy task cũ nếu có
        cancelTask(invoiceId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            cancelTask.run();
            pendingTasks.remove(invoiceId);
        }, delayMs, TimeUnit.MILLISECONDS);

        pendingTasks.put(invoiceId, future);
    }

    /**
     * Hủy scheduled task (khi nhân viên thanh toán hoặc hủy thủ công trước timeout).
     */
    public void cancelTask(int invoiceId) {
        ScheduledFuture<?> future = pendingTasks.remove(invoiceId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    /**
     * Kiểm tra đơn hàng có đang pending timer không.
     */
    public boolean hasPendingTask(int invoiceId) {
        ScheduledFuture<?> future = pendingTasks.get(invoiceId);
        return future != null && !future.isDone();
    }

    /**
     * Lấy thời gian còn lại (ms) của đơn hàng pending.
     */
    public long getRemainingTime(int invoiceId) {
        ScheduledFuture<?> future = pendingTasks.get(invoiceId);
        if (future != null && !future.isDone()) {
            return future.getDelay(TimeUnit.MILLISECONDS);
        }
        return 0;
    }

    /**
     * Tắt tất cả timer khi đóng ứng dụng.
     */
    public void shutdown() {
        scheduler.shutdownNow();
        pendingTasks.clear();
        synchronized (OrderTimerManager.class) {
            instance = null;
        }
    }
}
