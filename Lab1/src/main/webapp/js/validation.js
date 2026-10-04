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
    resetField(input);
    const value = Number(input.value.trim());
    
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

export function validateRoute(form) {
    clearValidation(form);
    const name = form.elements.name;
    if (!name.value.trim()) reject(name, 'Название маршрута не может быть пустым.');
    if (name.value.trim().length > 255) reject(name, 'Название маршрута должно содержать не более 255 символов.');
    finiteNumber(form.elements.coordX, 'Координата X');
    finiteNumber(form.elements.coordY, 'Координата Y');
    for (const [field, text, required] of [
        ['fromLocationId', 'Откуда', false],
        ['toLocationId', 'Куда', true]
    ]) {
        const input = form.elements[field];
        if (!input.value && !required) continue;
        const id = positiveInteger(input, text);
        if (!state.locations.some(location => location.id === id)) {
            reject(input, `${text}: выбранная локация не найдена.`);
        }
    }
    const distance = finiteNumber(form.elements.distance, 'Дистанция');
    if (distance <= 0) reject(form.elements.distance, 'Дистанция должна быть больше 0.');
    positiveInteger(form.elements.rating, 'Рейтинг');
}

export function routePayload(form) {
    return {
        name: form.elements.name.value.trim(),
        coordinates: { x: Number(form.elements.coordX.value), y: Number(form.elements.coordY.value) },
        fromLocationId: form.elements.fromLocationId.value ? Number(form.elements.fromLocationId.value) : null,
        toLocationId: Number(form.elements.toLocationId.value),
        distance: Number(form.elements.distance.value),
        rating: Number(form.elements.rating.value)
    };
}