function type(value) {
    return value.tagName;
  }

  function click(d) {
    console.log(d);
}

function force() {
    var width = $("#force-div").width();
    var height = $("#force-div").height();
    
    var svg = d3.select("#force-container").append("svg")
        .attr("class", "overlay")
        .attr("width", width)
        .attr("height", height);


    var force = d3.layout.force()
        .gravity(0.1)
        .distance(100)
        .charge(-700)
        .size([width, height]);

    var color = function (group) { 
        if (group == "edge") {
            return "#aaa";
        } else if (group == "device") {
            return "#fbc280";
        } else {
            return "#405275";
        }
    };


    //var nodes = [];
    //var links = [];
    

    function flatten(root) {
        var nodess = [];
        var i = 0;
       
        function recurse(node) {
          if (node.children) 
            node.children.forEach(recurse);
          if (!node.id) 
            node.id = ++i;
          nodess.push(node);
        }
       
        recurse(root);
        return nodess;
    } 
    var nodes = flatten(jsonData);
    var links = d3.layout.tree().links(nodes);

    force
        .nodes(nodes)
        .links(links)
        .start();

    var link = svg.selectAll(".link")
        .data(links)
        .enter().append("line")
        .attr("class", "link");

    var node = svg.selectAll(".node")
        .data(nodes)
        .enter().append("g")
        .attr("class", "node")
        .call(force.drag)
        .on('click', click);
    node.append('circle')
        .attr('r', 20)
        .style("stroke", function(d) {
            return d.actiontype == "delete" ? "red" : "steelblue";
        })
        .attr('fill', function (d) {
            return color(d.devicetype);
        });

    node.append("text")
        .attr("dx", 0)
        .attr("dy", 30)
        .style("font-family", "overwatch")
        .style("font-size", "12px") 
        .attr("text-anchor", "middle")
        .text(function (d) {
            return d.deviceid;
        });

    force.on("tick", function () {
        link.attr("x1", function (d) {
                return d.source.x;
            })
            .attr("y1", function (d) {
                return d.source.y;
            })
            .attr("x2", function (d) {
                return d.target.x;
            })
            .attr("y2", function (d) {
                return d.target.y;
            }); 
        node.attr("transform", function (d) { 
            if (d.actiontype == "create") {
                d.x = width * 0.1;
                d.y = height * 0.5;
            } else if (d.actiontype == "cloud") {
                d.x = width * 0.9;
                d.y = height * 0.5;
            }
            return "translate(" + d.x + "," + d.y + ")";
        });
    }); 
}
    