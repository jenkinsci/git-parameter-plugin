/**
 * The MIT License (MIT)
 *
 * Copyright (c) <2014-2015> <Ioannis Moutsatsos, Bruno P. Kinoshita>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
'use strict';
jQuery.noConflict();


var GitParameter = GitParameter || (function($) {
    var instance = {};

    function QuickFilter(selectElement, filterElement) {
        this.selectElement = selectElement;
        this.filterElement = filterElement;
        this.originalOptions = new Array();

        jQuery(this.filterElement).prop("disabled",true);

        this.initEventHandler();
    }

    QuickFilter.prototype.getSelectElement = function() {
        return this.selectElement;
    }

    QuickFilter.prototype.getFilterElement = function() {
        return this.filterElement;
    }

    QuickFilter.prototype.getOriginalOptions = function() {
        return this.originalOptions;
    }

    QuickFilter.prototype.initEventHandler = function() {
        var _self = this;

        jQuery(_self.getSelectElement()).on("filled", function() {
            var options = _self.getSelectElement().options;

            for (var i = 0; i < options.length; ++i) {
                _self.getOriginalOptions().push(options[i]);
            }
            jQuery(_self.getFilterElement()).prop("disabled",false);
            console.log("Quick Filter handler filled event." );
        });

        jQuery(_self.filterElement).keyup(function(e) {
            var filterElement = _self.getFilterElement();
            var filteredElement = _self.getSelectElement();
            var originalOptions = _self.getOriginalOptions();

            var search = filterElement.value.trim();
            var regex = new RegExp(search,"gi");

            var filteredOptions = Array();
            $.each(originalOptions, function(i) {
                var option = originalOptions[i];
                if(option.text.match(regex) !== null) {
                    filteredOptions.push(option)
                }
            });

            jQuery(filteredElement).children().remove();
            for (var i = 0; i < filteredOptions.length ; ++i) {
                var opt = document.createElement('option');
                opt.value = filteredOptions[i].value;
                opt.innerHTML = filteredOptions[i].innerHTML;
                jQuery(filteredElement).append(opt);
            }

            if (jQuery(filteredElement).get(0).length > 0) {
                jQuery(filteredElement).get(0).options[0].selected = true;
            }

            // Propagate the changes made by the filter
            console.log('Propagating change event after filtering');
            var e = jQuery.Event('change', {parameterName: 'Filter Element Event'});
            jQuery(filteredElement).trigger(e);
        });
    }

    instance.QuickFilter = QuickFilter;
    return instance;
})(jQuery);