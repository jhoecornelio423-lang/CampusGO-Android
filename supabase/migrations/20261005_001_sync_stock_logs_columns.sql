-- Migration: Fix stock_logs column compatibility for atomic RPCs
ALTER TABLE public.stock_logs ALTER COLUMN change_amount DROP NOT NULL;
ALTER TABLE public.stock_logs ALTER COLUMN previous_stock DROP NOT NULL;
ALTER TABLE public.stock_logs ALTER COLUMN new_stock DROP NOT NULL;

ALTER TABLE public.stock_logs ADD COLUMN IF NOT EXISTS quantity_changed integer;
ALTER TABLE public.stock_logs ADD COLUMN IF NOT EXISTS type text;
ALTER TABLE public.stock_logs ADD COLUMN IF NOT EXISTS notes text;

CREATE OR REPLACE FUNCTION public.sync_stock_logs_columns()
RETURNS trigger AS $$
BEGIN
    IF NEW.quantity_changed IS NOT NULL AND NEW.change_amount IS NULL THEN
        NEW.change_amount := NEW.quantity_changed;
    ELSIF NEW.change_amount IS NOT NULL AND NEW.quantity_changed IS NULL THEN
        NEW.quantity_changed := NEW.change_amount;
    END IF;

    IF NEW.notes IS NOT NULL AND NEW.reason IS NULL THEN
        NEW.reason := NEW.notes;
    ELSIF NEW.reason IS NOT NULL AND NEW.notes IS NULL THEN
        NEW.notes := NEW.reason;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_stock_logs ON public.stock_logs;
CREATE TRIGGER trg_sync_stock_logs
BEFORE INSERT ON public.stock_logs
FOR EACH ROW
EXECUTE FUNCTION public.sync_stock_logs_columns();
