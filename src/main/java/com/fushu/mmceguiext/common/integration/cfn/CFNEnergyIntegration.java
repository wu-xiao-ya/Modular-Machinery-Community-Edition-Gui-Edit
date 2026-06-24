package com.fushu.mmceguiext.common.integration.cfn;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.util.EnergyAccessHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class CFNEnergyIntegration {
    private static final String API_CLASS = "com.circulation.circulation_networks.api.API";
    private static final String MANAGER_INTERFACE = "com.circulation.circulation_networks.api.IEnergyHandlerManager";
    private static final String HANDLER_INTERFACE = "com.circulation.circulation_networks.api.IEnergyHandler";
    private static final String ENERGY_AMOUNT_CLASS = "com.circulation.circulation_networks.api.EnergyAmount";
    private static final int PRIORITY = 10_000;
    private static boolean attempted;

    private CFNEnergyIntegration() {
    }

    public static void registerIfPresent() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            Class<?> managerInterface = Class.forName(MANAGER_INTERFACE);
            Class<?> handlerInterface = Class.forName(HANDLER_INTERFACE);
            Class<?> energyAmountClass = Class.forName(ENERGY_AMOUNT_CLASS);

            Object managerProxy = Proxy.newProxyInstance(
                managerInterface.getClassLoader(),
                new Class<?>[]{managerInterface},
                new ManagerInvocationHandler(managerInterface, handlerInterface, energyAmountClass)
            );
            Method register = apiClass.getMethod("registerEnergyHandler", managerInterface);
            register.invoke(null, managerProxy);
            MMCEGuiExt.logger().info("Registered Circulation Flow Networks energy bridge for TileCustomHatch.");
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            MMCEGuiExt.logger().warn("Failed to register Circulation Flow Networks energy bridge: {}", t.toString());
        }
    }

    private static final class ManagerInvocationHandler implements InvocationHandler {
        private final Class<?> handlerInterface;
        private final Class<?> energyAmountClass;

        private ManagerInvocationHandler(Class<?> managerInterface, Class<?> handlerInterface, Class<?> energyAmountClass) {
            this.handlerInterface = handlerInterface;
            this.energyAmountClass = energyAmountClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, @Nullable Object[] args) throws Throwable {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            if ("isAvailable".equals(name)) {
                return args != null
                    && args.length == 1
                    && args[0] instanceof TileCustomHatch
                    && EnergyAccessHelper.resolveLongStorage(args[0]) != null;
            }
            if ("getEnergyHandlerClass".equals(name)) {
                return this.handlerInterface;
            }
            if ("getPriority".equals(name)) {
                return PRIORITY;
            }
            if ("newBlockEntityInstance".equals(name) || "newItemInstance".equals(name)) {
                return Proxy.newProxyInstance(
                    this.handlerInterface.getClassLoader(),
                    new Class<?>[]{this.handlerInterface},
                    new HandlerInvocationHandler(this.energyAmountClass)
                );
            }
            if ("resolveMappedHandler".equals(name) && args != null && args.length > 0) {
                return args[0];
            }
            if ("getUnit".equals(name)) {
                return "FE";
            }
            if ("getMultiplying".equals(name)) {
                return 1D;
            }
            if ("compareTo".equals(name)) {
                return Integer.compare(PRIORITY, resolveOtherPriority(args));
            }
            return defaultValue(method.getReturnType());
        }

        private static int resolveOtherPriority(@Nullable Object[] args) {
            if (args == null || args.length == 0 || args[0] == null) {
                return PRIORITY;
            }
            try {
                Object result = args[0].getClass().getMethod("getPriority").invoke(args[0]);
                return result instanceof Number ? ((Number) result).intValue() : PRIORITY;
            } catch (Exception ignored) {
                return PRIORITY;
            }
        }
    }

    private static final class HandlerInvocationHandler implements InvocationHandler {
        private final Class<?> energyAmountClass;
        @Nullable
        private TileCustomHatch tile;

        private HandlerInvocationHandler(Class<?> energyAmountClass) {
            this.energyAmountClass = energyAmountClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, @Nullable Object[] args) throws Throwable {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            if ("init".equals(name) && args != null && args.length > 0) {
                this.tile = args[0] instanceof TileCustomHatch ? (TileCustomHatch) args[0] : null;
                return null;
            }
            if ("clear".equals(name)) {
                this.tile = null;
                return null;
            }
            if ("resolveMappedHandler".equals(name)) {
                return proxy;
            }
            if ("shouldRunAsyncInit".equals(name)) {
                return false;
            }
            if ("asyncInit".equals(name)) {
                return null;
            }
            if ("receiveEnergy".equals(name)) {
                return toEnergyAmount(receiveOrExtract(args, true, false));
            }
            if ("extractEnergy".equals(name)) {
                return toEnergyAmount(receiveOrExtract(args, false, false));
            }
            if ("canReceiveValue".equals(name)) {
                return toEnergyAmount(queryAvailable(true));
            }
            if ("canExtractValue".equals(name)) {
                return toEnergyAmount(queryAvailable(false));
            }
            if ("canReceive".equals(name)) {
                return resolveStorage() != null && EnergyAccessHelper.getRemainingCapacity(this.tile) > 0L;
            }
            if ("canExtract".equals(name)) {
                return resolveStorage() != null && EnergyAccessHelper.getStored(this.tile) > 0L;
            }
            if ("requiresPairMatch".equals(name)) {
                return false;
            }
            if ("getType".equals(name)) {
                return resolveEnergyType();
            }
            return defaultValue(method.getReturnType());
        }

        private long receiveOrExtract(@Nullable Object[] args, boolean receive, boolean simulate) {
            if (resolveStorage() == null) {
                return 0L;
            }
            long amount = args != null && args.length > 0 ? energyAmountToLong(args[0]) : 0L;
            if (amount <= 0L) {
                return 0L;
            }
            return receive ? EnergyAccessHelper.receive(this.tile, amount, simulate) : EnergyAccessHelper.extract(this.tile, amount, simulate);
        }

        private long queryAvailable(boolean receive) {
            if (resolveStorage() == null) {
                return 0L;
            }
            return receive ? EnergyAccessHelper.getRemainingCapacity(this.tile) : EnergyAccessHelper.getStored(this.tile);
        }

        @Nullable
        private Object resolveStorage() {
            return this.tile == null ? null : EnergyAccessHelper.resolveLongStorage(this.tile);
        }

        private Object resolveEnergyType() throws Exception {
            String enumName;
            if (resolveStorage() == null) {
                enumName = "INVALID";
            } else {
                boolean canReceive = EnergyAccessHelper.getRemainingCapacity(this.tile) > 0L;
                boolean canExtract = EnergyAccessHelper.getStored(this.tile) > 0L;
                enumName = canReceive && canExtract ? "STORAGE" : canExtract ? "SEND" : canReceive ? "RECEIVE" : "INVALID";
            }
            Class<?> enumClass = Class.forName(HANDLER_INTERFACE + "$EnergyType");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object value = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), enumName);
            return value;
        }

        private Object toEnergyAmount(long value) throws Exception {
            return this.energyAmountClass.getMethod("obtain", long.class).invoke(null, Math.max(0L, value));
        }

        private static long energyAmountToLong(@Nullable Object energyAmount) {
            if (energyAmount == null) {
                return 0L;
            }
            if (energyAmount instanceof Number) {
                return Math.max(0L, ((Number) energyAmount).longValue());
            }
            try {
                Object result = energyAmount.getClass().getMethod("longValue").invoke(energyAmount);
                return result instanceof Number ? Math.max(0L, ((Number) result).longValue()) : 0L;
            } catch (Exception ignored) {
                return 0L;
            }
        }
    }

    private static Object invokeObjectMethod(Object proxy, Method method, @Nullable Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return "CFNEnergyIntegrationProxy";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(name)) {
            return args != null && args.length == 1 && proxy == args[0];
        }
        return null;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0F;
        }
        if (returnType == Double.TYPE) {
            return 0D;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}
