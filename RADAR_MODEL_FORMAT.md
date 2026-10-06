# Radar ONNX Model

Radar imports one `.onnx` file from Settings. It evaluates ordinary latitude/longitude points on a one-kilometer grid within five kilometers of the live driver location. It runs predictions for the selected service type and forecast time (the current hour or one hour ahead), then displays up to 30 highest positive results as map pins.

## Model interface

- Input: one float tensor shaped `[batch, 5]`.
- Feature order: latitude in degrees, longitude in degrees, ISO day of week (`1` Monday through `7` Sunday), hour of day (`0` through `23`), service code (`0` passenger, `1` food, `2` parcel).
- Output: one float order-demand estimate per input row, shaped `[batch]` or `[batch, 1]`.

The model must be trained with this same feature order and units. Radar does not read order history, H3 cells, ZIP archives, or calendar files at prediction time. Positive estimates are shown as pins; non-positive estimates are omitted.
