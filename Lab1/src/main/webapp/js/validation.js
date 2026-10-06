import { state } from './common.js';

function requireObject(value, text) {
    if (value === null || typeof value !== 'object' || Array.isArray(value)) {
        throw new Error(`${text}: требуется объект.`);
    }
}

function requireInput(input, text) {
    if (!input || typeof input.value !== 'string'
            || !['setCustomValidity', 'setAttribute', 'removeAttribute', 'focus', 'reportValidity']
                .every(method => typeof input[method] === 'function')) {
        throw new Error(`${text}: отсутствует или некорректно поле ввода.`);
    }
}

function requireForm(form) {
    if (!form || typeof form.querySelectorAll !== 'function'
            || !form.elements || typeof form.elements.namedItem !== 'function') {
        throw new Error('Отсутствует или некорректна форма.');
    }
}

function booleanOption(value, text) {
    if (typeof value !== 'boolean') throw new Error(`${text}: требуется логическое значение.`);
}

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
    requireForm(form);
    // Проверяем все поля до сброса, чтобы не оставить форму частично обработанной.
    const inputs = Array.from(form.querySelectorAll('input, select'));
    inputs.forEach(input => requireInput(input, 'Поле формы'));
    inputs.forEach(resetField);
}

export function initValidation() {
    for (const eventName of ['input', 'change']) {
        document.addEventListener(eventName, event => {
            if (typeof event.target?.matches === 'function' && event.target.matches('input, select')) {
                resetField(event.target);
            }
        });
    }
}

export function validateNumber(input, text = 'Число', options = {}) {
    requireInput(input, text);
    requireObject(options, 'Параметры проверки числа');
    const { integer = false, positive = false } = options;
    booleanOption(integer, 'Параметр integer');
    booleanOption(positive, 'Параметр positive');
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

export function locationId(input, text = 'Локация', options = {}) {
    requireInput(input, text);
    requireObject(options, 'Параметры проверки локации');
    const { required = true, locations = state.locations, excludeId = null } = options;
    booleanOption(required, 'Параметр required');
    if (!Array.isArray(locations)) throw new Error(`${text}: список локаций отсутствует или некорректен.`);
    for (const location of locations) {
        if (!location || typeof location !== 'object' || Array.isArray(location)
                || !Number.isSafeInteger(location.id) || location.id <= 0) {
            throw new Error(`${text}: список содержит некорректную локацию.`);
        }
    }
    const excluded = excludeId === null ? null : resourceId(excludeId, 'ID исключённой локации');
    if (!input.validity?.badInput && input.value.trim() === '' && !required) {
        resetField(input);
        return null;
    }
    const id = positiveInteger(input, text);
    if (id === excluded) reject(input, `${text}: выберите другую локацию.`);
    if (!locations.some(location => location.id === id)) {
        reject(input, `${text}: выбранная локация не найдена.`);
    }
    return id;
}

export function formField(form, field) {
    requireForm(form);
    if (typeof field !== 'string' || !field.trim()) throw new Error('Не задано имя поля формы.');
    // namedItem не путает поля с методами коллекции; дубли возвращают RadioNodeList.
    const input = form.elements.namedItem(field);
    requireInput(input, `Поле ${field}`);
    return input;
}

export function validateRoute(form) {
    clearValidation(form);
    const name = formField(form, 'name');
    if (!name.value.trim()) reject(name, 'Название маршрута не может быть пустым.');
    if (name.value.trim().length > 255) reject(name, 'Название маршрута должно содержать не более 255 символов.');
    const x = finiteNumber(formField(form, 'coordX'), 'Координата X');
    const y = finiteNumber(formField(form, 'coordY'), 'Координата Y');
    const fromLocationId = locationId(formField(form, 'fromLocationId'), 'Откуда', { required: false });
    const toLocationId = locationId(formField(form, 'toLocationId'), 'Куда');
    const distance = validateNumber(formField(form, 'distance'), 'Дистанция', { positive: true });
    const rating = positiveInteger(formField(form, 'rating'), 'Рейтинг');
    return { name: name.value.trim(), coordinates: { x, y }, fromLocationId, toLocationId, distance, rating };
}

export function routePayload(form) {
    return validateRoute(form);
}
