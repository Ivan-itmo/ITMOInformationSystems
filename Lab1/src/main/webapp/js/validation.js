import { state } from './common.js';

function reject(input, message) {
    input.setCustomValidity(message);
    input.setAttribute('aria-invalid', 'true');
    input.focus();
    input.reportValidity();
    throw new Error(message);
}

function resetField(input) {
    input.setCustomValidity('');
    input.removeAttribute('aria-invalid');
}

export function clearValidation(form) {
    form.querySelectorAll('input, select').forEach(resetField);
}

export function initValidation() {
    for (const eventName of ['input', 'change']) {
        document.addEventListener(eventName, event => {
            if (event.target.matches('input, select')) resetField(event.target);
        });
    }
}

export function validateNumber(input, text, { integer = false, positive = false } = {}) {
    if (!input || typeof input.value !== 'string') {
        throw new Error(`${text}: отсутствует поле ввода.`);
    }
    resetField(input);
    if (input.validity?.badInput) reject(input, `${text}: введите число в правильном формате.`);
    const raw = input.value.trim();
    if (!raw) reject(input, `${text}: заполните обязательное поле.`);
    const pattern = integer ? /^\d+$/ : /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/;
    if (!pattern.test(raw)) {
        reject(input, `${text}: ${integer ? 'введите целое число' : 'введите число в правильном формате'}.`);
    }
    const value = Number(raw);
    
    if (!Number.isFinite(value)) {
        reject(input, `${text}: введите конечное число.`);
    }
    if (integer && !Number.isSafeInteger(value)) {
        reject(input, `${text}: введите целое число.`);
    }
    if (positive && value <= 0) {
        reject(input, `${text}: значение должно быть больше 0.`);
    }
    return value;
}

export const finiteNumber = (input, text) => validateNumber(input, text);
export const positiveInteger = (input, text) => validateNumber(input, text, { integer: true, positive: true });

export function resourceId(value, text = 'ID') {
    if (!['string', 'number'].includes(typeof value) || !/^\d+$/.test(String(value))
            || !Number.isSafeInteger(Number(value)) || Number(value) <= 0) {
        throw new Error(`${text}: требуется целое число больше 0 в безопасном диапазоне JavaScript.`);
    }
    return Number(value);
}

export function locationId(input, text, { required = true, locations = state.locations, excludeId = null } = {}) {
    if (input && !input.validity?.badInput && input.value === '' && !required) {
        resetField(input);
        return null;
    }
    const id = positiveInteger(input, text);
    if (id === excludeId) reject(input, `${text}: выберите другую локацию.`);
    if (!locations.some(location => location.id === id)) {
        reject(input, `${text}: выбранная локация не найдена.`);
    }
    return id;
}

function routeField(form, field) {
    const input = form.elements[field];
    if (!input || typeof input.value !== 'string') {
        throw new Error(`Отсутствует поле маршрута: ${field}.`);
    }
    return input;
}

export function validateRoute(form) {
    clearValidation(form);
    const name = routeField(form, 'name');
    if (!name.value.trim()) reject(name, 'Название маршрута не может быть пустым.');
    if (name.value.trim().length > 255) reject(name, 'Название маршрута должно содержать не более 255 символов.');
    const x = finiteNumber(routeField(form, 'coordX'), 'Координата X');
    const y = finiteNumber(routeField(form, 'coordY'), 'Координата Y');
    const fromLocationId = locationId(routeField(form, 'fromLocationId'), 'Откуда', { required: false });
    const toLocationId = locationId(routeField(form, 'toLocationId'), 'Куда');
    const distance = validateNumber(routeField(form, 'distance'), 'Дистанция', { positive: true });
    const rating = positiveInteger(routeField(form, 'rating'), 'Рейтинг');
    return { name: name.value.trim(), coordinates: { x, y }, fromLocationId, toLocationId, distance, rating };
}

export function routePayload(form) {
    return validateRoute(form);
}
