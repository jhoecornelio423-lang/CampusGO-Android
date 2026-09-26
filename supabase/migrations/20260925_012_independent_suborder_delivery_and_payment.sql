-- 20260925_012_independent_suborder_delivery_and_payment.sql
-- Permite que cada subpedido guarde su propio punto de encuentro, horario y método de pago independiente.

CREATE OR REPLACE FUNCTION public.checkout_order_atomic(
    p_order_id UUID,
    p_meeting_point_id VARCHAR,
    p_meeting_point_name VARCHAR,
    p_scheduled_time VARCHAR,
    p_payment_method VARCHAR,
    p_notes TEXT,
    p_suborders JSONB
)
RETURNS JSONB AS $$
DECLARE
    v_buyer_id UUID;
    v_order_code TEXT;
    v_grand_total NUMERIC(10, 2) := 0;
    v_sub JSONB;
    v_item JSONB;
    v_sub_id UUID;
    v_seller_id UUID;
    v_subtotal NUMERIC(10, 2);
    v_prod_stock INT;
    v_prod_price NUMERIC(10, 2);
    v_prod_active BOOLEAN;
    v_prod_name VARCHAR;
    v_prod_seller UUID;
    v_seller_accepting BOOLEAN;
    v_first_seller_id UUID := NULL;
    v_existing_order_id UUID;
    v_sub_pm VARCHAR;
    v_sub_mp_id UUID;
    v_sub_mp_name TEXT;
    v_sub_time TEXT;
BEGIN
    -- Bandera local para omitir triggers heredados
    PERFORM set_config('vallego.atomic_checkout', 'true', true);

    v_buyer_id := auth.uid();
    IF v_buyer_id IS NULL THEN
        RAISE EXCEPTION 'UNAUTHENTICATED: Usuario no autenticado.';
    END IF;

    SELECT id INTO v_existing_order_id FROM public.orders WHERE id = p_order_id;
    IF v_existing_order_id IS NOT NULL THEN
        RETURN jsonb_build_object(
            'success', true,
            'order_id', p_order_id,
            'is_duplicate', true,
            'message', 'Pedido ya registrado previamente.'
        );
    END IF;

    IF p_meeting_point_name IS NULL OR length(trim(p_meeting_point_name)) = 0 THEN
        RAISE EXCEPTION 'INVALID_DATA: Debe seleccionar un punto de encuentro válido.';
    END IF;
    IF p_scheduled_time IS NULL OR length(trim(p_scheduled_time)) = 0 THEN
        RAISE EXCEPTION 'INVALID_DATA: Debe seleccionar un horario de entrega válido.';
    END IF;

    IF p_suborders IS NULL OR jsonb_array_length(p_suborders) = 0 THEN
        RAISE EXCEPTION 'INVALID_DATA: El pedido debe contener al menos un producto.';
    END IF;

    -- Validar stock y disponibilidad de todos los productos y puestos
    FOR v_sub IN SELECT * FROM jsonb_array_elements(p_suborders)
    LOOP
        v_seller_id := (v_sub->>'seller_id')::UUID;
        IF v_first_seller_id IS NULL THEN
            v_first_seller_id := v_seller_id;
        END IF;

        IF v_seller_id = v_buyer_id THEN
            RAISE EXCEPTION 'SELF_PURCHASE: No puedes realizar un pedido a tu propio emprendimiento.';
        END IF;

        SELECT accepting_orders INTO v_seller_accepting FROM public.profiles WHERE id = v_seller_id;
        IF v_seller_accepting = false THEN
            RAISE EXCEPTION 'SELLER_CLOSED: Un emprendedor del carrito actualmente no está aceptando pedidos.';
        END IF;

        v_subtotal := 0;
        FOR v_item IN SELECT * FROM jsonb_array_elements(v_sub->'items')
        LOOP
            SELECT stock, price, is_active, name, seller_id
            INTO v_prod_stock, v_prod_price, v_prod_active, v_prod_name, v_prod_seller
            FROM public.products
            WHERE id = (v_item->>'product_id')::UUID
            FOR UPDATE;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'PRODUCT_NOT_FOUND: El producto seleccionado no existe.';
            END IF;

            IF v_prod_seller != v_seller_id THEN
                RAISE EXCEPTION 'INVALID_SELLER: El producto % no pertenece al emprendedor indicado.', v_prod_name;
            END IF;

            IF NOT v_prod_active OR v_prod_stock <= 0 THEN
                RAISE EXCEPTION 'PRODUCT_UNAVAILABLE: El producto "%" ya no se encuentra disponible.', v_prod_name;
            END IF;

            IF v_prod_stock < (v_item->>'quantity')::INT THEN
                RAISE EXCEPTION 'INSUFFICIENT_STOCK: Stock insuficiente para "%". Disponibles: %, solicitados: %',
                    v_prod_name, v_prod_stock, (v_item->>'quantity')::INT;
            END IF;

            UPDATE public.products
            SET stock = stock - (v_item->>'quantity')::INT,
                is_active = (stock - (v_item->>'quantity')::INT > 0),
                updated_at = now()
            WHERE id = (v_item->>'product_id')::UUID;

            INSERT INTO public.stock_logs (product_id, quantity_changed, type, notes)
            VALUES (
                (v_item->>'product_id')::UUID,
                -(v_item->>'quantity')::INT,
                'sale',
                'Reserva de stock por pedido #' || p_order_id
            );

            v_subtotal := v_subtotal + (v_prod_price * (v_item->>'quantity')::INT);
        END LOOP;

        v_grand_total := v_grand_total + v_subtotal;
    END LOOP;

    -- Insertar orden principal
    INSERT INTO public.orders (
        id,
        buyer_id,
        seller_id,
        total_price,
        delivery_place,
        meeting_point_id,
        meeting_point_name,
        scheduled_time,
        notes,
        payment_method,
        status
    ) VALUES (
        p_order_id,
        v_buyer_id,
        v_first_seller_id,
        v_grand_total,
        p_meeting_point_name || ' (' || p_scheduled_time || ')',
        p_meeting_point_id,
        p_meeting_point_name,
        p_scheduled_time,
        p_notes,
        p_payment_method,
        'pending'
    ) RETURNING order_code INTO v_order_code;

    -- Insertar subpedidos con sus datos independientes
    FOR v_sub IN SELECT * FROM jsonb_array_elements(p_suborders)
    LOOP
        v_sub_id := COALESCE((v_sub->>'id')::UUID, gen_random_uuid());
        v_seller_id := (v_sub->>'seller_id')::UUID;
        v_sub_pm := COALESCE(v_sub->>'payment_method', p_payment_method, 'EFECTIVO');

        -- Obtener punto de encuentro del subpedido o caer al general
        v_sub_mp_id := NULL;
        IF (v_sub->>'meeting_point_id') IS NOT NULL AND length(trim(v_sub->>'meeting_point_id')) > 0 THEN
            BEGIN
                v_sub_mp_id := (v_sub->>'meeting_point_id')::UUID;
            EXCEPTION WHEN OTHERS THEN
                v_sub_mp_id := NULL;
            END;
        END IF;

        IF v_sub_mp_id IS NULL AND p_meeting_point_id IS NOT NULL AND length(trim(p_meeting_point_id)) > 0 THEN
            BEGIN
                v_sub_mp_id := p_meeting_point_id::UUID;
            EXCEPTION WHEN OTHERS THEN
                v_sub_mp_id := NULL;
            END;
        END IF;

        v_sub_mp_name := COALESCE(v_sub->>'meeting_point_name', p_meeting_point_name);
        v_sub_time := COALESCE(v_sub->>'scheduled_time', p_scheduled_time);

        v_subtotal := 0;
        FOR v_item IN SELECT * FROM jsonb_array_elements(v_sub->'items')
        LOOP
            SELECT price INTO v_prod_price FROM public.products WHERE id = (v_item->>'product_id')::UUID;
            v_subtotal := v_subtotal + (v_prod_price * (v_item->>'quantity')::INT);
        END LOOP;

        INSERT INTO public.sub_orders (
            id,
            order_id,
            seller_id,
            subtotal_amount,
            status,
            payment_method,
            is_payment_confirmed,
            is_delivery_confirmed,
            stock_reserved,
            meeting_point_id,
            meeting_point_name,
            scheduled_time
        ) VALUES (
            v_sub_id,
            p_order_id,
            v_seller_id,
            v_subtotal,
            'pending',
            v_sub_pm,
            false,
            false,
            true,
            v_sub_mp_id,
            v_sub_mp_name,
            v_sub_time
        );

        FOR v_item IN SELECT * FROM jsonb_array_elements(v_sub->'items')
        LOOP
            SELECT price INTO v_prod_price FROM public.products WHERE id = (v_item->>'product_id')::UUID;
            INSERT INTO public.order_items (
                id,
                order_id,
                sub_order_id,
                product_id,
                quantity,
                price_at_sale
            ) VALUES (
                COALESCE((v_item->>'id')::UUID, gen_random_uuid()),
                p_order_id,
                v_sub_id,
                (v_item->>'product_id')::UUID,
                (v_item->>'quantity')::INT,
                v_prod_price
            );
        END LOOP;
    END LOOP;

    RETURN jsonb_build_object(
        'success', true,
        'order_id', p_order_id,
        'order_code', v_order_code,
        'total_amount', v_grand_total,
        'status', 'pending',
        'is_duplicate', false
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
