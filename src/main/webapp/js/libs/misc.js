//* miscellaneous scripts for registration *//

function clearText(field) {
    if (field.defaultValue == field.value) {
        field.value = '';
    } else if (field.value == '') {
        field.value = field.defaultValue;
    }
}

function windowpop(url, width, height) {
    event.preventDefault();
    var leftPosition, topPosition;
    //Allow for borders.
    leftPosition = (window.screen.width / 2) - ((width / 2) + 10);
    //Allow for title and status bars.
    topPosition = (window.screen.height / 2) - ((height / 2) + 50);
    //Open the window.
    window.open(url, "Window2", "status=no,height=" + height + ",width=" + width + ",resizable=yes,left=" + leftPosition + ",top=" + topPosition + ",screenX=" + leftPosition + ",screenY=" + topPosition + ",toolbar=no,menubar=no,scrollbars=no,location=no,directories=no");
}

if (typeof String.prototype.startsWith != 'function') {
    String.prototype.startsWith = function (str) {
        return (this.lastIndexOf(str, 0) === 0);
    };
}

function addFormValidation(formSelector, validation, errorsEncoded) {
    var validator = $(formSelector).validate($.extend({
        errorClass: 'form-control-feedback',
        validClass: 'form-control-feedback',

        getExtValue: function (elementName, key) {
            if (!this.ext) {
                return null;
            }

            var elementExt = this.ext[elementName];
            return (elementExt) ? elementExt[key] : null;
        },
        getExtErrorContainer: function (element) {
            return this.getExtValue(element.attr('name'), 'errorContainer');
        },
        getExtShowValidGlyph: function (elementName) {
            return this.getExtValue(elementName, 'showValidGlyph');
        },

        getElements: function (element) {
            if (element.type === 'radio') {
                return this.findByName(element.name);
            } else {
                return $(element);
            }
        },

        errorPlacement: function (label, element) {
            var errorContainer = this.getExtErrorContainer(element);
            if (errorContainer) {
                errorContainer.append(label);
                return;
            }

            var formGroup = element.closest('[class~="form-group"]');
            var secondary = formGroup.find('[class~="secondary"]'); // if wrapped in secondary (e.g. check boxes)
            var inputGroup = formGroup.find('[class~="input-group"]'); // if horizontal controls
            var insertAfterElement = element;
            if (secondary.length > 0) {
                insertAfterElement = secondary;
            }
            if (inputGroup.length > 0) {
                insertAfterElement = inputGroup;
            }
            label.insertAfter(insertAfterElement);
        },
        success: function (label) {
            var forName = label.attr('for');
            if (forName && this.getExtShowValidGlyph(forName) === false) {
                return;
            }

            var group = label.closest('[class~="form-group"]');
            group.addClass('has-feedback');
            if (group.data('show-valid-glyph') !== false) {
                label.addClass('glyphicon glyphicon-ok');
            }
        },
        highlight: function (element, errorClass, validClass) {
            $(element.form).find('label[for=' + element.id + ']').removeClass('glyphicon glyphicon-ok');
            this.settings.getElements(element).closest('[class~="form-group"]').addClass('has-error').removeClass('has-success has-feedback');
        },
        unhighlight: function (element, errorClass, validClass) {
            this.settings.getElements(element).closest('[class~="form-group"]').removeClass('has-error').addClass('has-success');
        }
    }, validation));

    if (errorsEncoded) {
        var errors = eval('(' + decodeURIComponent(errorsEncoded) + ')');
        validator.showErrors(errors);
    }

    return validator;
}